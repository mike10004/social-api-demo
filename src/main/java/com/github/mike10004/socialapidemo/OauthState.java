package com.github.mike10004.socialapidemo;

import java.net.URL;

public interface OauthState {
    String getKey();
    URL getAuthorizationUrl();
}
