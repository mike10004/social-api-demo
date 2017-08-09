package com.github.mike10004.socialapidemo;

import com.google.common.math.LongMath;
import com.google.common.util.concurrent.RateLimiter;

import java.time.Duration;

import static com.google.common.base.Preconditions.checkArgument;

public class CountingTitrator implements Titrator {

    private final int numDosesPerPeriod;
    private final RateLimiter rateLimiter;
    private long numDosesAvailable;
    private transient final Object doseLock;

    protected CountingTitrator(int numDosesPerPeriod, Duration period) {
        checkArgument(numDosesPerPeriod > 0, "numDosesPerPeriod must be positive, not %s", numDosesPerPeriod);
        checkArgument(!period.isNegative() && !period.isZero(), "period must have positive length: %s", period);
        this.numDosesPerPeriod = numDosesPerPeriod;
        double numSeconds = Titrator.durationToFractionalSeconds(period);
        rateLimiter = RateLimiter.create(1 / numSeconds);
        numDosesAvailable = 0L;
        doseLock = new Object();
    }

    /**
     * Consumes a dose from this titrator, blocking until a dose is available.
     */
    public void consume() {
        synchronized (doseLock) {
            if (numDosesAvailable == 0) { // then refill
                rateLimiter.acquire();
                numDosesAvailable = LongMath.checkedAdd(numDosesAvailable, numDosesPerPeriod);
            }
            numDosesAvailable = LongMath.checkedSubtract(numDosesAvailable, 1);
        }
    }

    /**
     * Consumes a dose from this titrator if one is available.
     * @return true if a dose was available (and consumed), false otherwise
     */
    public boolean tryConsume() {
        synchronized (doseLock) {
            return numDosesAvailable > 0;
        }
    }

}
