package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.RateLimiter;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class SteadyThrottler implements Throttler {

    private ImmutableMap<String, RateLimiter> rateLimiterMap;
    private RateLimiter defaultRateLimiter;

    public SteadyThrottler(Map<String, RateLimiter> rateLimiterMap, RateLimiter defaultRateLimiter) {
        this.rateLimiterMap = ImmutableMap.copyOf(rateLimiterMap);
        this.defaultRateLimiter = defaultRateLimiter;
    }

    @Override
    public void delay(@Nullable String category) {
        RateLimiter rateLimiter = defaultRateLimiter;
        if (category != null) {
            rateLimiter = rateLimiterMap.getOrDefault(category, defaultRateLimiter);
        }
        rateLimiter.acquire();
    }

    public static Builder builder(RateLimiter defaultRateLimiter) {
        return new Builder(defaultRateLimiter);
    }

    public static class Builder {

        private final Map<String, RateLimiter> rateLimiterMap = new HashMap<>();
        private final RateLimiter defaultRateLimiter;

        private Builder(RateLimiter defaultRateLimiter) {
            this.defaultRateLimiter = checkNotNull(defaultRateLimiter);
        }

        @SuppressWarnings("UnusedReturnValue")
        public Builder limit(String category, double permitsPerSecond) {
            rateLimiterMap.put(checkNotNull(category, "use default rate limiter for null category"), RateLimiter.create(permitsPerSecond));
            return this;
        }

        public SteadyThrottler build() {
            return new SteadyThrottler(rateLimiterMap, defaultRateLimiter);
        }
    }
}
