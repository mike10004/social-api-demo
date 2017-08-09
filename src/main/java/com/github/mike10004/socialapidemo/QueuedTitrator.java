package com.github.mike10004.socialapidemo;

import com.google.common.util.concurrent.RateLimiter;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Queue;

import static com.google.common.base.Preconditions.checkArgument;

public class QueuedTitrator implements Titrator {

    private static final Object DOSE = new Object();

    private final int numDosesPerPeriod;
    private final RateLimiter rateLimiter;
    private final Queue<Object> doseQueue;
    private transient final Object queueLock;

    protected QueuedTitrator(int numDosesPerPeriod, Duration period) {
        checkArgument(numDosesPerPeriod > 0, "numDosesPerPeriod must be positive, not %s", numDosesPerPeriod);
        checkArgument(!period.isNegative() && !period.isZero(), "period must have positive length: %s", period);
        this.numDosesPerPeriod = numDosesPerPeriod;
        double numSeconds = durationToFractionalSeconds(period);
        rateLimiter = RateLimiter.create(1 / numSeconds);
        doseQueue = new LinkedList<>();
        queueLock = new Object();
    }

    /**
     * Consumes a dose from this titrator, blocking until a dose is available.
     */
    public void consume() {
        synchronized (queueLock) {
            if (doseQueue.peek() == null) { // then refill the queue
                rateLimiter.acquire();
                for (int i = 0; i < numDosesPerPeriod; i++) {
                    doseQueue.add(DOSE);
                }
            }
            doseQueue.remove();
        }
    }

    /**
     * Consumes a dose from this titrator if one is available.
     * @return true if a dose was available (and consumed), false otherwise
     */
    public boolean tryConsume() {
        synchronized (queueLock) {
            return doseQueue.poll() != null;
        }
    }

    static double durationToFractionalSeconds(Duration period) {
        return period.getSeconds() + period.getNano() / 1000000000d;
    }
}
