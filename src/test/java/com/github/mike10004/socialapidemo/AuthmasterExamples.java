package com.github.mike10004.socialapidemo;

import com.google.gson.GsonBuilder;
import facebook4j.Facebook;
import twitter4j.Twitter;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("unused")
public class AuthmasterExamples {

    public static class F {
        public static void main(String[] args) throws Exception {
            Tests.OauthClientConfig oauthClient = Tests.loadExampleOauthCreds(Program.Sns.facebook);
            Facebook facebookClient = Program.Sns.facebook.buildClient(oauthClient);
            FacebookAuthmaster authmaster = new FacebookAuthmaster(facebookClient);
            int port = checkNotNull(oauthClient.redirectUriPort, "redirectUriPort").intValue();
            AccessBadge badge = authmaster.authorize(port);
            new GsonBuilder().setPrettyPrinting().create().toJson(badge, System.out);
            System.out.println();
        }
    }

    public static class T {
        public static void main(String[] args) throws Exception {
            OauthConfig oauthConfig = Tests.loadExampleOauthCreds(Program.Sns.twitter);
            Twitter twitterClient = Program.Sns.twitter.buildClient(oauthConfig);
            TwitterAuthmaster authmaster = new TwitterAuthmaster(twitterClient);
            int port = Tests.provideUsablePort();
            AccessBadge badge = authmaster.authorize(port);
            new GsonBuilder().setPrettyPrinting().create().toJson(badge, System.out);
            System.out.println();
        }
    }

}
