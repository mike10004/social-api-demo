package com.github.mike10004.socialapidemo;

import com.google.common.annotations.VisibleForTesting;
import twitter4j.JSONException;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

import java.io.PrintStream;

public class TwitterDemo extends Demonstrator<Twitter> {
    public TwitterDemo(Twitter client) {
        super(client);
    }

    @VisibleForTesting
    public TwitterDemo(Twitter client, PrintStream output) {
        super(client, output);
    }

    @Override
    public void demonstrate() throws ActivityException {
        try {
            User user = client.verifyCredentials();
            String userJson = user.getJson().toString(2);
            output.println(userJson);
        } catch (TwitterException | JSONException e) {
            throw new ActivityException(e);
        }
    }
}
