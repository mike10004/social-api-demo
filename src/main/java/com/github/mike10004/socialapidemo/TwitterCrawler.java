package com.github.mike10004.socialapidemo;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.JSONException;
import twitter4j.JSONObject;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterResponse;
import twitter4j.User;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class TwitterCrawler extends Crawler<Twitter, TwitterException> {

    private static final int TWITTER_WINDOW_MINUTES = 15;

    public TwitterCrawler(Twitter client, CrawlerConfig crawlerConfig) {
        this(client, crawlerConfig, buildThrottler(crawlerConfig), buildAssetProcessor(crawlerConfig));
    }

    @VisibleForTesting
    TwitterCrawler(Twitter client, CrawlerConfig crawlerConfig, Throttler throttler, AssetProcessor assetProcessor) {
        super(client, crawlerConfig, throttler, assetProcessor);
    }

    @Override
    public void crawl() throws CrawlerException, TwitterException {
        User me = throttle(Cat.account_verify_credentials, client::verifyCredentials);
        assetProcessor.process(me, Cat.account_verify_credentials);
    }

    @Override
    protected RateLimitReaction<TwitterException> getRateLimitReaction(TwitterException exception) {
        return RateLimitReaction.fail(exception);
    }

    @Override
    protected boolean isRateLimitException(TwitterException exception) {
        return exception.exceededRateLimitation();
    }

    /**
     * Category string constants holder.
     */
    @SuppressWarnings("unused")
    private static final class Cat {

        private Cat() {}

        public static final String account_verify_credentials = "account/verify_credentials";
        public static final String application_rate_limit_status = "application/rate_limit_status";
        public static final String favorites_list = "favorites/list";
        public static final String followers_ids = "followers/ids";
        public static final String followers_list = "followers/list";
        public static final String friends_ids = "friends/ids";
        public static final String friends_list = "friends/list";
        public static final String friendships_show = "friendships/show";
        public static final String geo_id__place_id = "geo/id/:place_id";
        public static final String help_configuration = "help/configuration";
        public static final String help_languages = "help/languages";
        public static final String help_privacy = "help/privacy";
        public static final String help_tos = "help/tos";
        public static final String lists_list = "lists/list";
        public static final String lists_members = "lists/members";
        public static final String lists_members_show = "lists/members/show";
        public static final String lists_memberships = "lists/memberships";
        public static final String lists_ownerships = "lists/ownerships";
        public static final String lists_show = "lists/show";
        public static final String lists_statuses = "lists/statuses";
        public static final String lists_subscribers = "lists/subscribers";
        public static final String lists_subscribers_show = "lists/subscribers/show";
        public static final String lists_subscriptions = "lists/subscriptions";
        public static final String search_tweets = "search/tweets";
        public static final String statuses_lookup = "statuses/lookup";
        public static final String statuses_mentions_timeline = "statuses/mentions_timeline";
        public static final String statuses_retweeters_ids = "statuses/retweeters/ids";
        public static final String statuses_retweets_of_me = "statuses/retweets_of_me";
        public static final String statuses_retweets__id = "statuses/retweets/:id";
        public static final String statuses_show__id = "statuses/show/:id";
        public static final String statuses_user_timeline = "statuses/user_timeline";
        public static final String trends_available = "trends/available";
        public static final String trends_closest = "trends/closest";
        public static final String trends_place = "trends/place";
        public static final String users_lookup = "users/lookup";
        public static final String users_search = "users/search";
        public static final String users_show = "users/show";
        public static final String users_suggestions = "users/suggestions";
        public static final String users_suggestions__slug = "users/suggestions/:slug";
        public static final String users_suggestions__slug_members = "users/suggestions/:slug/members";
    }

    /**
     * Builds a twitter throttler that throttles calls based on the published rate limits.
     * @return a rate-limiting throttler
     */
    protected static Throttler buildDefaultTwitterThrottler() {
        // https://dev.twitter.com/rest/public/rate-limits
        // default: 15 requests per 15 minute window
        //        = 15 requests per 15*60 seconds
        //        = 1 request per 60 seconds
        //        = 1/60 requests per second
        SteadyThrottler.Builder b = SteadyThrottler.builder(RateLimiter.create(computePermitsPerSecondForRequestsPerMinutes(15, 15)));
        for (TwitterEndpointLimit tel : getTwitterEndpointLimits()) {
            b.limit(tel.category, computePermitsPerSecondForRequestsPerMinutes(tel.numRequestsPerWindow, TWITTER_WINDOW_MINUTES));
        }
        return b.build();
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

    protected static Throttler buildThrottler(CrawlerConfig crawlerConfig) {
        if (crawlerConfig.throttleStrategy != null) {
            switch (crawlerConfig.throttleStrategy) {
                case NONE:
                    return Throttler.inactive();
            }
        }
        return buildDefaultTwitterThrottler();
    }

    protected static AssetProcessor buildAssetProcessor(CrawlerConfig crawlerConfig) {
        if (crawlerConfig.processorSpecUri == null) {
            return new LoggingAssetProcessor();
        }
        URI storageSpecUri = URI.create(crawlerConfig.processorSpecUri);
        if ("file".equals(storageSpecUri.getScheme())) {
            File root = new File(storageSpecUri);
            FileStoringAssetProcessor.AssetSerializer serializer = new TwitterAssetSerializer();
            return new FileStoringAssetProcessor(root.toPath(), serializer);
        }
        throw new IllegalStateException("not handled: " + crawlerConfig.processorSpecUri);
    }

    private static class TwitterAssetSerializer implements FileStoringAssetProcessor.AssetSerializer {

        private final Logger log = LoggerFactory.getLogger(getClass());

        @Override
        public ByteSource serialize(Object asset) {
            try {
                if (asset instanceof TwitterResponse) {
                    JSONObject jsonObject = ((TwitterResponse) asset).getJson();
                    String json = jsonObject.toString(2);
                    return serialize(json);
                } else {
                    log.debug("not serializing foreign object of {}", (asset == null ? "<null>" : asset.getClass()));
                }
            } catch (RuntimeException | JSONException e) {
                log.info("failed to serialize " + asset, e);
            }
            return null;
        }
    }



}
