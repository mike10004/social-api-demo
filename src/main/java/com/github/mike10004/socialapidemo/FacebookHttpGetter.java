package com.github.mike10004.socialapidemo;

import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.internal.http.HttpClientWrapper;
import facebook4j.internal.http.HttpResponse;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;

public class FacebookHttpGetter implements HttpGetter {

    private final Facebook facebookClient;

    public FacebookHttpGetter(Facebook facebookClient) {
        this.facebookClient = facebookClient;
    }

    public SimpleResponse executeGet(URL url) throws IOException {
        try {
            Class<?> implClass = Class.forName("facebook4j.FacebookBaseImpl");
            Field httpField = implClass.getDeclaredField("http");
            httpField.setAccessible(true);
            facebook4j.internal.http.HttpClientWrapper httpClient = (HttpClientWrapper) httpField.get(facebookClient);
            HttpResponse response = httpClient.get(url.toString());
            String text = response.asString();
            int status = response.getStatusCode();
            return new SimpleResponse(url, status, text);
        } catch (NoSuchFieldException | FacebookException | ClassNotFoundException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
