package com.github.mike10004.socialapidemo;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.RateLimiter;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Throttler that throttles calls based on the published Twitter API rate limits.
 */
public class TwitterThrottler extends SteadyThrottler {

    private static final int TWITTER_WINDOW_MINUTES = 15;

    /**
     * Margin to add to throttler's throttling. We were seeing a lot of rate limit violations
     * where we only had to wait one more second until the rate limit reset, which means that
     * Twitter's accounting of calls is slightly off or our throttling is just slightly too
     * fast. Adding a delay of 1.1 seconds should avoid the issue.
     */
    private static final Duration THROTTLER_MARGIN_DURATION = Duration.ofMillis(1100);

    public TwitterThrottler() {
        this(Sleeper.system());
    }

    @VisibleForTesting
    TwitterThrottler(Sleeper marginSleeper) {
        super(buildPublishedTwitterRateLimitsMap(), buildDefaultRateLimiter(), THROTTLER_MARGIN_DURATION, marginSleeper);
    }

    static Map<String, RateLimiter> buildPublishedTwitterRateLimitsMap() {
        ImmutableMap.Builder<String, RateLimiter> b = ImmutableMap.builder();
        for (TwitterEndpointLimit tel : getTwitterEndpointLimits()) {
            b.put(tel.category, RateLimiter.create(computePermitsPerSecondForRequestsPerMinutes(tel.numRequestsPerWindow, TWITTER_WINDOW_MINUTES)));
        }
        return b.build();
    }

    private static RateLimiter buildDefaultRateLimiter() {
        // https://dev.twitter.com/rest/public/rate-limits
        // default: 15 requests per 15 minute window
        //        = 15 requests per 15*60 seconds
        //        = 1 request per 60 seconds
        //        = 1/60 requests per second
        return RateLimiter.create(computePermitsPerSecondForRequestsPerMinutes(15, 15));
    }

    private static double computePermitsPerSecondForRequestsPerMinutes(int numRequests, int numMinutes) {
        int numSeconds = numMinutes * 60;
        return (double) numRequests / (double) numSeconds;
    }

    private static List<TwitterEndpointLimit> getTwitterEndpointLimits() {
        return Arrays.asList(
                new TwitterEndpointLimit("account/verify_credentials", 75),
                new TwitterEndpointLimit("application/rate_limit_status", 180),
                new TwitterEndpointLimit("favorites/list", 75),
                new TwitterEndpointLimit("followers/ids", 15),
                new TwitterEndpointLimit("followers/list", 15),
                new TwitterEndpointLimit("friends/ids", 15),
                new TwitterEndpointLimit("friends/list", 15),
                new TwitterEndpointLimit("friendships/show", 180),
                new TwitterEndpointLimit("geo/id/:place_id", 75),
                new TwitterEndpointLimit("help/configuration", 15),
                new TwitterEndpointLimit("help/languages", 15),
                new TwitterEndpointLimit("help/privacy", 15),
                new TwitterEndpointLimit("help/tos", 15),
                new TwitterEndpointLimit("lists/list", 15),
                new TwitterEndpointLimit("lists/members", 900),
                new TwitterEndpointLimit("lists/members/show", 15),
                new TwitterEndpointLimit("lists/memberships", 75),
                new TwitterEndpointLimit("lists/ownerships", 15),
                new TwitterEndpointLimit("lists/show", 75),
                new TwitterEndpointLimit("lists/statuses", 900),
                new TwitterEndpointLimit("lists/subscribers", 180),
                new TwitterEndpointLimit("lists/subscribers/show", 15),
                new TwitterEndpointLimit("lists/subscriptions", 15),
                new TwitterEndpointLimit("search/tweets", 180),
                new TwitterEndpointLimit("statuses/lookup", 900),
                new TwitterEndpointLimit("statuses/mentions_timeline", 75),
                new TwitterEndpointLimit("statuses/retweeters/ids", 75),
                new TwitterEndpointLimit("statuses/retweets_of_me", 75),
                new TwitterEndpointLimit("statuses/retweets/:id", 75),
                new TwitterEndpointLimit("statuses/show/:id", 900),
                new TwitterEndpointLimit("statuses/user_timeline", 900),
                new TwitterEndpointLimit("trends/available", 75),
                new TwitterEndpointLimit("trends/closest", 75),
                new TwitterEndpointLimit("trends/place", 75),
                new TwitterEndpointLimit("users/lookup", 900),
                new TwitterEndpointLimit("users/search", 900),
                new TwitterEndpointLimit("users/show", 900),
                new TwitterEndpointLimit("users/suggestions", 15),
                new TwitterEndpointLimit("users/suggestions/:slug", 15),
                new TwitterEndpointLimit("users/suggestions/:slug/members", 15)
        );
    }

    private static class TwitterEndpointLimit {
        public final String category;
        public final int numRequestsPerWindow;

        private TwitterEndpointLimit(String category, int numRequestsPerWindow) {
            this.category = category;
            this.numRequestsPerWindow = numRequestsPerWindow;
        }
    }


}
