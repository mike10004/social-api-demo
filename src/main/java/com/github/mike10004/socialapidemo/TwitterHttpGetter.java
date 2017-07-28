package com.github.mike10004.socialapidemo;

import twitter4j.HttpResponse;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.io.IOException;
import java.net.URL;

public class TwitterHttpGetter implements HttpGetter {

    private final Twitter twitterClient;

    public TwitterHttpGetter(Twitter twitterClient) {
        this.twitterClient = twitterClient;
    }

    @Override
    public SimpleResponse executeGet(URL url) throws IOException {
        try {
            twitter4j.HttpClientConfiguration httpConf = twitterClient.getConfiguration().getHttpClientConfiguration();
            twitter4j.HttpClient httpClient = twitter4j.HttpClientFactory.getInstance(httpConf);
            HttpResponse response = httpClient.get(url.toString());
            String text = response.asString();
            int status = response.getStatusCode();
            return new SimpleResponse(url, status, text);
        } catch (TwitterException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new RuntimeException(e);
        }
    }
}
