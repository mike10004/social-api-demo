package com.github.mike10004.socialapidemo;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.math.IntMath;
import com.google.common.primitives.Longs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.RateLimitStatus;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import twitter4j.api.FriendsFollowersResources;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class TwitterCrawler extends Crawler<Twitter, TwitterException> {

    private static final Logger log = LoggerFactory.getLogger(TwitterCrawler.class);

    public TwitterCrawler(Twitter client, CrawlerConfig crawlerConfig) {
        super(client, crawlerConfig);
    }

    @Override
    protected Iterator<CrawlNode<?, TwitterException>> getSeedGenerator() {
        List<CrawlNode<?, TwitterException>> crawlNodes;
        TwitterCrawlerStrategy strategy = crawlerConfig.getTwitterCrawlerStrategy();
        if (strategy.firstUserId == null) {
            crawlNodes = ImmutableList.of(new AuthenticatedUserProfileNode());
        } else {
            crawlNodes = ImmutableList.of(new OtherUserProfileNode(Long.parseLong(strategy.firstUserId)));
        }
        return crawlNodes.iterator();
    }

    private static final String CAT_EDGESET = "edges";

    abstract class UserProfileNode extends CrawlNode<User, TwitterException> {

        private static final int MAX_FOLLOWSHIP_BRANCHES = 8;

        public UserProfileNode(@Nullable String category) {
            super(category);
        }
        @Override
        public Iterable<String> getLineage(User asset) {
            return ImmutableList.of(String.valueOf(asset.getId()));
        }

        @Override
        public Collection<CrawlNode<?, TwitterException>> findNextTargets(User asset) throws TwitterException {
            FriendsFollowersResources ff = client.friendsFollowers();
            twitter4j.IDs followerIds = throttle(Cat.followers_ids, () -> ff.getFollowersIDs(asset.getId(), -1));
            twitter4j.IDs followeeIds = throttle(Cat.friends_ids, () -> ff.getFriendsIDs(asset.getId(), -1));
            EdgeSet<?> edges = createEdgeSet(asset, followerIds, followeeIds);
            getAssetProcessor().process(edges, CAT_EDGESET);
            Set<Long> ids = new LinkedHashSet<>(IntMath.checkedAdd(followerIds.getIDs().length, followeeIds.getIDs().length));
            LongStream.of(followerIds.getIDs()).limit(MAX_FOLLOWSHIP_BRANCHES).forEach(ids::add);
            LongStream.of(followeeIds.getIDs()).limit(MAX_FOLLOWSHIP_BRANCHES).forEach(ids::add);
            return ids.stream().map(OtherUserProfileNode::new).collect(Collectors.toList());
        }

        @Override
        public String identify(User asset) {
            return String.valueOf("u/" + asset.getId());
        }
    }

    private static List<String> toStringList(long[] numbers) {
        return Longs.asList(numbers).stream().map(Object::toString).collect(Collectors.toList());
    }

    protected EdgeSet<TwitterRelationshipType> createEdgeSet(User source, twitter4j.IDs followerIds, twitter4j.IDs followeeIds) {
        String sourceId = String.valueOf(source.getId());
        long[] followerIdValues = followerIds.getIDs(), followeeIdValues = followeeIds.getIDs();
        Multimap<TwitterRelationshipType, String> relationships = ArrayListMultimap.create(TwitterRelationshipType.NUM_VALUES, Math.max(followeeIdValues.length, followerIdValues.length));
        relationships.putAll(TwitterRelationshipType.sourceFollows, toStringList(followeeIdValues));
        relationships.putAll(TwitterRelationshipType.followsSource, toStringList(followerIdValues));
        return new EdgeSet<>(sourceId, relationships);
    }

    public enum TwitterRelationshipType {
        sourceFollows, followsSource;

        static final int NUM_VALUES = values().length;
    }

    class AuthenticatedUserProfileNode extends UserProfileNode {

        public AuthenticatedUserProfileNode() {
            super(Cat.account_verify_credentials);
        }

        @Override
        public User call() throws TwitterException {
            return client.verifyCredentials();
        }

    }

    class OtherUserProfileNode extends UserProfileNode {

        private final long userId;

        OtherUserProfileNode(long userId) {
            super(Cat.users_show);
            this.userId = userId;
        }

        @Override
        public User call() throws TwitterException {
            return client.showUser(userId);
        }

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

    @Override
    protected void maybeHandleRateLimitException(TwitterException exception, RemainingActionsStatus remaining) {
        if (exception.exceededRateLimitation()) {
            RateLimitStatus rateLimitStatus = exception.getRateLimitStatus();
            int secondsUntilReset = rateLimitStatus.getSecondsUntilReset();
            Duration sleepDuration = Duration.ofSeconds(secondsUntilReset + 1L);
            log.info("rate limit exception heard; sleeping for {} seconds ({})", sleepDuration.getSeconds(), sleepDuration);
            if (remaining == RemainingActionsStatus.SOME) {
                crawlerConfig.getSleeper().sleep(sleepDuration);
            }
        }
    }
}
