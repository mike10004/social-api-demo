package com.github.mike10004.socialapidemo;

import facebook4j.Facebook;
import facebook4j.FacebookFactory;
import facebook4j.conf.Configuration;
import facebook4j.conf.ConfigurationBuilder;
import org.slf4j.LoggerFactory;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public interface SnsClientBuilder<T> {

    T buildClient(OauthClient client, @Nullable AccessBadge badge);

    static SnsClientBuilder<Facebook> facebook() {
        String ALL_PERMISSIONS =
                "public_profile,"
                        + "email,"
                        + "user_about_me,"
                        + "user_actions.books,"
                        + "user_actions.fitness,"
                        + "user_actions.music,"
                        + "user_actions.news,"
                        + "user_actions.video,"
                        + "user_birthday,"
                        + "user_education_history,"
                        + "user_events,"
                        + "user_friends,"
                        + "user_games_activity,"
                        + "user_hometown,"
                        + "user_likes,"
                        + "user_location,"
                        + "user_photos,"
                        + "user_posts,"
                        + "user_relationship_details,"
                        + "user_relationships,"
                        + "user_religion_politics,"
                        + "user_status,"
                        + "user_tagged_places,"
                        + "user_videos,"
                        + "user_website,"
                        + "user_work_history";
        return new SnsClientBuilder<Facebook>() {
            @Override
            public Facebook buildClient(OauthClient client, @Nullable AccessBadge badge) {
                @Nullable String accessToken = badge == null ? null : checkNotNull(badge.accessToken, "badge not null but access token is null");
                return buildFacebookClient(client, accessToken);
            }

            private Facebook buildFacebookClient(OauthClient oauthClient, String accessToken) {
                checkNotNull(oauthClient, "oauth client id/secret");
                if (oauthClient.clientId == null || oauthClient.clientSecret == null) {
                    LoggerFactory.getLogger(getClass()).warn("client id/secret not set");
                }
                ConfigurationBuilder cb = new ConfigurationBuilder();
                cb.setOAuthAppId(oauthClient.clientId)
                        .setOAuthAppSecret(oauthClient.clientSecret)
                        .setOAuthPermissions(ALL_PERMISSIONS);
                Configuration conf = cb.build();
                FacebookFactory factory = new FacebookFactory(conf);
                return accessToken == null
                        ? factory.getInstance()
                        : factory.getInstance(new facebook4j.auth.AccessToken(accessToken, null));
            }

        };
    }

    static SnsClientBuilder<Twitter> twitter() {
        return new SnsClientBuilder<Twitter>() {
            @Override
            public Twitter buildClient(OauthClient client, @Nullable AccessBadge badge) {
                twitter4j.auth.AccessToken officialToken = null;
                if (badge != null) {
                    officialToken = new twitter4j.auth.AccessToken(badge.accessToken, checkNotNull(badge.accessSecret, "access token secret"));
                }
                return buildTwitterClient(client, officialToken);
            }

            private Twitter buildTwitterClient(OauthClient oauthClient, @Nullable twitter4j.auth.AccessToken accessToken) {
                if (oauthClient.clientId == null || oauthClient.clientSecret == null) {
                    LoggerFactory.getLogger(getClass()).warn("client id/secret not set");
                }
                TwitterFactory tf = new TwitterFactory(new twitter4j.conf.ConfigurationBuilder()
                        .setOAuthConsumerKey(oauthClient.clientId)
                        .setOAuthConsumerSecret(oauthClient.clientSecret)
                        .build());
                return accessToken == null
                        ? tf.getInstance()
                        : tf.getInstance(accessToken);
            }
        };
    }
}
