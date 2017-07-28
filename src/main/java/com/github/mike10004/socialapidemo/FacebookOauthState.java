package com.github.mike10004.socialapidemo;

import java.net.URI;
import java.net.URL;

public class FacebookOauthState implements OauthState {

    private final String stateKey;
    private final URI redirectUri;
    private final URL authorizationUrl;

    public FacebookOauthState(String stateKey, URL authorizationUrl, URI redirectUri) {
        this.stateKey = stateKey;
        this.redirectUri = redirectUri;
        this.authorizationUrl = authorizationUrl;
    }

    @Override
    public String getKey() {
        return stateKey;
    }

    @Override
    public URL getAuthorizationUrl() {
        return authorizationUrl;
    }

    public URI getRedirectUri() {
        return redirectUri;
    }
}
