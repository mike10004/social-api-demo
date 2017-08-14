package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.RateLimiter;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class SteadyThrottler implements Throttler {

    private final ImmutableMap<String, RateLimiter> rateLimiterMap;
    private final RateLimiter defaultRateLimiter;
    private final Sleeper marginSleeper;
    private final Duration marginDuration;

    /**
     * Constructs an instance.
     * @param rateLimiterMap Map of rate limiter to use, by category
     * @param defaultRateLimiter the rate limiter to use if no map entry exists for category
     * @param marginDuration duration to sleep before each delay, as an extra margin to stay under the rate limits
     * @param marginSleeper sleeper to use when sleeping for the margin duration
     */
    public SteadyThrottler(Map<String, RateLimiter> rateLimiterMap, RateLimiter defaultRateLimiter, Duration marginDuration, Sleeper marginSleeper) {
        this.rateLimiterMap = ImmutableMap.copyOf(rateLimiterMap);
        this.defaultRateLimiter = defaultRateLimiter;
        this.marginDuration = checkNotNull(marginDuration);
        this.marginSleeper = checkNotNull(marginSleeper);
    }

    @Override
    public void delay(@Nullable String category) {
        RateLimiter rateLimiter = defaultRateLimiter;
        if (category != null) {
            rateLimiter = rateLimiterMap.getOrDefault(category, defaultRateLimiter);
        }
        maybeSleepForMargin();
        rateLimiter.acquire();
    }

    private void maybeSleepForMargin() {
        if (!marginDuration.isZero()) {
            marginSleeper.sleep(marginDuration);
        }
    }

}
