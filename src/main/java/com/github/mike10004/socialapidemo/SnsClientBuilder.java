package com.github.mike10004.socialapidemo;

import facebook4j.Facebook;
import facebook4j.FacebookFactory;
import facebook4j.conf.Configuration;
import org.slf4j.LoggerFactory;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public interface SnsClientBuilder<T> {

    T buildClient(OauthConfig client, @Nullable AccessBadge badge);

    default T buildClient(OauthCredentials oauthCredentials) {
        return buildClient(oauthCredentials, oauthCredentials.badge);
    }

    static SnsClientBuilder<Facebook> facebook() {
        return facebook(new facebook4j.conf.ConfigurationBuilder());
    }

    static SnsClientBuilder<Facebook> facebook(facebook4j.conf.ConfigurationBuilder cb) {
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
            public Facebook buildClient(OauthConfig client, @Nullable AccessBadge badge) {
                @Nullable String accessToken = badge == null ? null : checkNotNull(badge.accessToken, "badge not null but access token is null");
                return buildFacebookClient(client, accessToken);
            }

            private Facebook buildFacebookClient(OauthConfig oauthConfig, String accessToken) {
                checkNotNull(oauthConfig, "oauth client id/secret");
                if (oauthConfig.clientId == null || oauthConfig.clientSecret == null) {
                    LoggerFactory.getLogger(getClass()).warn("client id/secret not set");
                }
                cb.setOAuthAppId(oauthConfig.clientId)
                        .setOAuthAppSecret(oauthConfig.clientSecret)
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
        return twitter(new twitter4j.conf.ConfigurationBuilder());
    }

    static SnsClientBuilder<Twitter> twitter(twitter4j.conf.ConfigurationBuilder cb) {
        return new SnsClientBuilder<Twitter>() {
            @Override
            public Twitter buildClient(OauthConfig client, @Nullable AccessBadge badge) {
                twitter4j.auth.AccessToken officialToken = null;
                if (badge != null) {
                    officialToken = new twitter4j.auth.AccessToken(badge.accessToken, checkNotNull(badge.accessSecret, "access token secret"));
                }
                return buildTwitterClient(client, officialToken);
            }

            private Twitter buildTwitterClient(OauthConfig oauthConfig, @Nullable twitter4j.auth.AccessToken accessToken) {
                if (oauthConfig.clientId == null || oauthConfig.clientSecret == null) {
                    LoggerFactory.getLogger(getClass()).warn("client id/secret not set");
                }
                TwitterFactory tf = new TwitterFactory(cb
                        .setOAuthConsumerKey(oauthConfig.clientId)
                        .setOAuthConsumerSecret(oauthConfig.clientSecret)
                        .build());
                return accessToken == null
                        ? tf.getInstance()
                        : tf.getInstance(accessToken);
            }
        };
    }
}
