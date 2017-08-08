package com.github.mike10004.socialapidemo;

import facebook4j.Facebook;

import java.io.IOException;

public class FacebookHttpGetterTest extends HttpGetterTestBase {

    @Override
    protected HttpGetter buildGetter() throws IOException {
        OauthConfig oauthConfig = Tests.loadExampleOauthCreds(Program.Sns.facebook);
        Facebook facebookClient = Program.Sns.facebook.buildClient(oauthConfig);
        return new FacebookHttpGetter(facebookClient);
    }
}