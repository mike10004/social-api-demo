package com.github.mike10004.socialapidemo;

import twitter4j.auth.RequestToken;

import java.net.MalformedURLException;
import java.net.URL;

public class TwitterOauthState implements OauthState {

    private final String stateKey;
    private final RequestToken requestToken;

    public TwitterOauthState(String stateKey, RequestToken requestToken) {
        this.requestToken = requestToken;
        this.stateKey = stateKey;
    }

    @Override
    public String getKey() {
        return stateKey;
    }

    @Override
    public URL getAuthorizationUrl() {
        try {
            return new URL(requestToken.getAuthorizationURL());
        } catch (MalformedURLException e) {
            throw new Authmaster.AuthmasterException(e);
        }
    }

    public RequestToken getRequestToken() {
        return requestToken;
    }
}
