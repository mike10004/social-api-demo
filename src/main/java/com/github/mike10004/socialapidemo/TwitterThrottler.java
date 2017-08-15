package com.github.mike10004.socialapidemo;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.RateLimiter;

import java.time.Duration;
import java.util.Map;

/**
 * Throttler that throttles calls based on the published Twitter API rate limits.
 */
public abstract class TwitterThrottler<T> extends CategorizingThrottler<T> {

    private static final int TWITTER_WINDOW_MINUTES = 15;

    // https://dev.twitter.com/rest/public/rate-limits
    // default: 15 requests per 15 minute window
    //        = 15 requests per 15*60 seconds
    //        = 1 request per 60 seconds
    //        = 1/60 requests per second
    @SuppressWarnings("UnnecessaryBoxing")
    private static final Integer DEFAULT_REQUESTS_PER_WINDOW = Integer.valueOf(15);

    /**
     * Margin to add to throttler's throttling. We were seeing a lot of rate limit violations
     * where we only had to wait one more second until the rate limit reset, which means that
     * Twitter's accounting of calls is slightly off or our throttling is just slightly too
     * fast. We want to add a safety margin of about 1.5 seconds to each 15-minute window, and the
     * slowest rate at which calls are made is 15 per window, which means we want to divide
     * that 1.5 seconds over 15 calls, which is 0.1 seconds per call.
     */
    private static final Duration THROTTLER_MARGIN_DURATION = Duration.ofMillis(100);

    private final Map<String, Integer> endpointLimitMap;

    public TwitterThrottler() {
        this(Sleeper.system());
    }

    @VisibleForTesting
    TwitterThrottler(Sleeper marginSleeper) {
        super(THROTTLER_MARGIN_DURATION, marginSleeper);
        this.endpointLimitMap = buildPublishedTwitterRateLimitsMap();
    }

    @Override
    protected T createPermitAcquirer(String category) {
        category = Strings.nullToEmpty(category);
        int limit = endpointLimitMap.getOrDefault(category, DEFAULT_REQUESTS_PER_WINDOW).intValue();
        return createPermitAcquirer(limit);
    }

    protected abstract T createPermitAcquirer(int maxRequestsPerWindow);

    public static Throttler steady() {
        return steady(Sleeper.system());
    }

    static Throttler steady(Sleeper marginSleeper) {
        return new SteadyTwitterThrottler(marginSleeper);
    }

    public static Throttler greedy() {
        return greedy(Sleeper.system());
    }

    static Throttler greedy(Sleeper marginSleeper) {
        return new GreedyTwitterThrottler(marginSleeper);
    }

    private static class SteadyTwitterThrottler extends TwitterThrottler<RateLimiter> {
        public SteadyTwitterThrottler(Sleeper marginSleeper) {
            super(marginSleeper);
        }

        @Override
        protected RateLimiter createPermitAcquirer(int numRequestsPerWindow) {
            return RateLimiter.create(computePermitsPerSecondForRequestsPerMinutes(numRequestsPerWindow, TWITTER_WINDOW_MINUTES));
        }

        private static double computePermitsPerSecondForRequestsPerMinutes(int numRequests, int numMinutes) {
            int numSeconds = numMinutes * 60;
            return (double) numRequests / (double) numSeconds;
        }

        @Override
        protected void acquire(RateLimiter permitAcquirer) {
            permitAcquirer.acquire();
        }
    }

    private static class GreedyTwitterThrottler extends TwitterThrottler<Titrator> {

        private static final Duration TWITTER_WINDOW_DURATION = Duration.ofMinutes(TWITTER_WINDOW_MINUTES);

        public GreedyTwitterThrottler(Sleeper marginSleeper) {
            super(marginSleeper);
        }

        @Override
        protected Titrator createPermitAcquirer(int maxRequestsPerWindow) {
            return Titrator.create(maxRequestsPerWindow, TWITTER_WINDOW_DURATION);
        }

        @Override
        protected void acquire(Titrator permitAcquirer) {
            permitAcquirer.consume();
        }
    }

    static Map<String, Integer> buildPublishedTwitterRateLimitsMap() {
        return ImmutableMap.<String, Integer>builder()
                .put("account/verify_credentials", 75)
                .put("application/rate_limit_status", 180)
                .put("favorites/list", 75)
                .put("followers/ids", 15)
                .put("followers/list", 15)
                .put("friends/ids", 15)
                .put("friends/list", 15)
                .put("friendships/show", 180)
                .put("geo/id/:place_id", 75)
                .put("help/configuration", 15)
                .put("help/languages", 15)
                .put("help/privacy", 15)
                .put("help/tos", 15)
                .put("lists/list", 15)
                .put("lists/members", 900)
                .put("lists/members/show", 15)
                .put("lists/memberships", 75)
                .put("lists/ownerships", 15)
                .put("lists/show", 75)
                .put("lists/statuses", 900)
                .put("lists/subscribers", 180)
                .put("lists/subscribers/show", 15)
                .put("lists/subscriptions", 15)
                .put("search/tweets", 180)
                .put("statuses/lookup", 900)
                .put("statuses/mentions_timeline", 75)
                .put("statuses/retweeters/ids", 75)
                .put("statuses/retweets_of_me", 75)
                .put("statuses/retweets/:id", 75)
                .put("statuses/show/:id", 900)
                .put("statuses/user_timeline", 900)
                .put("trends/available", 75)
                .put("trends/closest", 75)
                .put("trends/place", 75)
                .put("users/lookup", 900)
                .put("users/search", 900)
                .put("users/show", 900)
                .put("users/suggestions", 15)
                .put("users/suggestions/:slug", 15)
                .put("users/suggestions/:slug/members", 15)
                .build();
    }

}
