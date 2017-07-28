package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableMap;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

public class TwitterAuthmaster extends Authmaster<TwitterOauthState> {

    private final Twitter twitterClient;

    public TwitterAuthmaster(Twitter twitterClient) {
        this.twitterClient = twitterClient;
    }

    @Override
    public TwitterOauthState createOauthState(URL redirectUri, String stateKey) {
        try {
            URI parameterizedRedirectUri = new URIBuilder(redirectUri.toURI())
                    .addParameter("state", stateKey)
                    .build();
            RequestToken requestToken = twitterClient.getOAuthRequestToken(parameterizedRedirectUri.toString());
            return new TwitterOauthState(stateKey, requestToken);
        } catch (URISyntaxException | TwitterException e) {
            throw new AuthmasterException(e);
        }
    }

    @Override
    protected String getCodeParamName() {
        return "oauth_verifier";
    }

    @Override
    protected AccessBadge exchangeCodeForAccessToken(TwitterOauthState state, String oauthVerifier) {
        try {
            AccessToken accessToken = twitterClient.getOAuthAccessToken(state.getRequestToken(), oauthVerifier);
            Map<String, String> extras = ImmutableMap.of("userId", String.valueOf(accessToken.getUserId()), "screenName", accessToken.getScreenName());
            return new AccessBadge(accessToken.getToken(), accessToken.getTokenSecret(), null, extras);
        } catch (TwitterException e) {
            throw new AuthmasterException(e);
        }
    }
}
