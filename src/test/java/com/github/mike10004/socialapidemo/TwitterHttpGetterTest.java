package com.github.mike10004.socialapidemo;

import twitter4j.Twitter;

import java.io.IOException;

public class TwitterHttpGetterTest extends HttpGetterTestBase {
    @Override
    protected HttpGetter buildGetter() throws IOException {
        Twitter twitterClient = Program.Sns.twitter.buildClient(Tests.loadExampleOauthCreds(Program.Sns.twitter));
        return new TwitterHttpGetter(twitterClient);
    }

}