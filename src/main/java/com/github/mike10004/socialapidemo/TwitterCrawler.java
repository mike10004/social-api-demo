package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class TwitterCrawler extends Crawler<Twitter, TwitterException> {

    public TwitterCrawler(Twitter client, CrawlerConfig crawlerConfig) {
        super(client, crawlerConfig);
    }

    @Override
    public void crawl() throws CrawlerException, TwitterException {
        acquire(new Action<User, TwitterException>(Cat.account_verify_credentials){
            @Override
            public Iterable<String> getLineage(User asset) {
                return ImmutableList.of(String.valueOf(asset.getId()));
            }

            @Override
            public User call() throws TwitterException {
                return client.verifyCredentials();
            }
        });
    }

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

}
