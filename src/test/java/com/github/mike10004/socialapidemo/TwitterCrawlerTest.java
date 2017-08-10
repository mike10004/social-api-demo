package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.google.gson.Gson;
import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import twitter4j.Twitter;
import twitter4j.conf.ConfigurationBuilder;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertFalse;

public class TwitterCrawlerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public NanoHttpdRule httpdRule = new NanoHttpdRule();

    private static boolean hasParameter(NanoHTTPD.IHTTPSession session, String paramName, Predicate<? super String> paramValueTest) {
        Map<String, List<String>> queryParams = session.getParameters();
        return queryParams.getOrDefault(paramName, ImmutableList.of()).stream().anyMatch(paramValueTest);
    }

    @SuppressWarnings("SameParameterValue")
    private static boolean hasParameter(NanoHTTPD.IHTTPSession session, String paramName, String paramValue) {
        return hasParameter(session, paramName, paramValue::equals);
    }

    @Nullable
    private static String getParameter(NanoHTTPD.IHTTPSession session, String paramName) {
        return session.getParameters().getOrDefault(paramName, ImmutableList.of()).stream().findFirst().orElse(null);
    }

    private static final String EMPTY_IDS_JSON = "{\n" +
            "  \"previous_cursor\": 0,\n" +
            "  \"ids\": [],\n" +
            "  \"previous_cursor_str\": \"0\",\n" +
            "  \"next_cursor\": 0,\n" +
            "  \"next_cursor_str\": \"0\"\n" +
            "}";

    @Nullable
    private static String getResponseJson(NanoHTTPD.IHTTPSession session) {
        String userId = getParameter(session, "user_id");
        Gson gson = new Gson();
        switch (session.getUri()) {
            case "/friends/ids.json":
                if ("749236595".equals(userId)) {
                    return Tests.loadResourceAsStringQuietly("/twitter/friends-ids.json");
                } else {
                    return EMPTY_IDS_JSON;
                }
            case "/followers/ids.json":
                if ("749236595".equals(userId)) {
                    return Tests.loadResourceAsStringQuietly("/twitter/followers-ids.json");
                } else {
                    return EMPTY_IDS_JSON;
                }
            case "/account/verify_credentials.json":
                return Tests.loadResourceAsStringQuietly("/twitter/verify-credentials-1.json");
            case "/users/show.json":
                return gson.toJson(ImmutableMap.of("id", Long.valueOf(checkNotNull(userId)), "screen_name", "user" + userId));
            default:
                return null;
        }

    }

    @Before
    public void setUp() throws Exception {
        httpdRule.handle(session -> {
            String json = getResponseJson(session);
            return json == null ? null : NanoHttpdRule.respond(200, "application/json", json);
        });
    }

    @Test
    public void crawl() throws Exception {
        OauthCredentials oauthConfig = Tests.loadExampleOauthCreds(Program.Sns.twitter);
        ConfigurationBuilder cb = new ConfigurationBuilder();
        HostAndPort address = httpdRule.getSocketAddress();
        String restBaseUrl = new URL("http", address.getHost(), address.getPort(), "/").toString();
        cb.setRestBaseURL(restBaseUrl);
        Twitter client = SnsClientBuilder.twitter(cb).buildClient(oauthConfig);
        checkState(oauthConfig.clientId != null, "oauth client id absent");
        checkState(oauthConfig.clientSecret != null, "oauth client secret absent");
        checkState(oauthConfig.badge != null, "oauth access badge absent");
        checkState(oauthConfig.badge.accessToken != null, "oauth access badge token absent");
        checkState(oauthConfig.badge.accessSecret != null, "oauth access badge secret absent");
        File storageRoot = temporaryFolder.getRoot();
        System.out.format("%s is storage root%n", storageRoot);
        CrawlerConfig crawlerConfig = new CrawlerConfig(){
            @Override
            protected AssetProcessor buildAssetProcessor() {
                return new FileStoringAssetProcessor(storageRoot.toPath(), new TwitterAssetSerializer());
            }
        };
        TwitterCrawler crawler = new TwitterCrawler(client, crawlerConfig);
        crawler.crawl();
        System.out.format("httpdrule heard %d requests and matched %d%n", httpdRule.getNumRequestsHeard(), httpdRule.getNumRequestsMatched());
        checkState(httpdRule.getNumRequestsMatched() > 0, "no requests matched by httpdrule");
        Collection<File> assets = FileUtils.listFiles(storageRoot, null, true);
        assets.forEach(System.out::println);
        assertFalse("expect something downloaded", assets.isEmpty());
        System.out.format("crawl complete, %d items downloaded%n", assets.size());
    }

}
