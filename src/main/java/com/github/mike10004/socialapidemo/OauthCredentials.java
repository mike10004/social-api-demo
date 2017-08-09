package com.github.mike10004.socialapidemo;

public class OauthCredentials extends OauthConfig {

    public AccessBadge badge;

    public OauthCredentials(String clientId, String clientSecret, AccessBadge badge) {
        super(clientId, clientSecret);
        this.badge = badge;
    }
}
