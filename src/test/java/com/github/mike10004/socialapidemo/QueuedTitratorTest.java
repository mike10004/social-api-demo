package com.github.mike10004.socialapidemo;

import com.google.common.base.Stopwatch;
import com.google.common.math.LongMath;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static org.junit.Assert.*;

public class QueuedTitratorTest {

    private static class ConsumeTestCase {

        private final int dosesPerPeriod;
        private final Duration period;
        private final int numToConsume;

        /**
         * We might encounter trouble at very low periods due to the processing time of things
         * like synchronization and queue operations.
         */
        private static final Duration MIN_PERIOD = Duration.ofMillis(100);

        public ConsumeTestCase(int dosesPerPeriod, Duration period, int numToConsume) {
            this.dosesPerPeriod = dosesPerPeriod;
            this.period = period;
            checkArgument(period.compareTo(MIN_PERIOD) >= 0);
            this.numToConsume = numToConsume;
        }

    }

    @Test(timeout = 10000)
    public void consume() throws Exception {
        List<ConsumeTestCase> testCases = Arrays.asList(
                new ConsumeTestCase( 5, Duration.ofMillis(250), 6 ),
                new ConsumeTestCase( 1, Duration.ofMillis(250), 1 ),
                new ConsumeTestCase( 1, Duration.ofMillis(250), 2 ),
                new ConsumeTestCase( 100, Duration.ofMillis(250), 50 ),
                new ConsumeTestCase( 100, Duration.ofMillis(250), 225 ),
                new ConsumeTestCase( 5, Duration.ofSeconds(2), 7 )
        );

        for (ConsumeTestCase testCase : testCases) {
            int dosesPerPeriod = testCase.dosesPerPeriod, numToConsume = testCase.numToConsume;
            Duration period = testCase.period;
            System.out.format("dosesPerPeriod=%d, period=%s, numToConsume=%d; ", dosesPerPeriod, period, numToConsume);
            QueuedTitrator titrator = new QueuedTitrator(dosesPerPeriod, period);
            // should take < 4 seconds to consume 10 permits
            Stopwatch watch = Stopwatch.createStarted();
            for (int numConsumed = 0; numConsumed < numToConsume; numConsumed++) {
                titrator.consume();
            }
            Duration elapsed = watch.elapsed();
            System.out.format("elapsed = %s%n", elapsed);
            long elapsedMillis = elapsed.toMillis();
            long maxExpectedMillis = LongMath.checkedMultiply(Math.round(Math.ceil((double) numToConsume / (double) dosesPerPeriod)), period.toMillis());
            assertTrue("less than " + maxExpectedMillis + "ms: " + elapsedMillis, elapsedMillis < maxExpectedMillis);
        }
    }

    @Test(timeout = 500)
    public void tryConsume() throws Exception {
        int dosesPerPeriod = 1;
        Duration period = Duration.ofSeconds(10);
        QueuedTitrator titrator = new QueuedTitrator(dosesPerPeriod, period);
        titrator.consume();
        assertFalse(titrator.tryConsume());
    }
}