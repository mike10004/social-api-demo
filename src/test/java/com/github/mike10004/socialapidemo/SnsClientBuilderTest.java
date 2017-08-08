package com.github.mike10004.socialapidemo;

import facebook4j.Facebook;
import org.junit.Test;

import java.net.URL;

public class SnsClientBuilderTest {
    @Test
    public void facebook() throws Exception {
        OauthConfig client = new OauthConfig("abc", "def");
        String accessToken = "ghi";
        Facebook fb = SnsClientBuilder.facebook().buildClient(client, new AccessBadge(accessToken, null, null));
        String url = fb.getOAuthAuthorizationURL("http://localhost:12345/");
        System.out.println(new URL(url)); // ok if no exception
    }

}