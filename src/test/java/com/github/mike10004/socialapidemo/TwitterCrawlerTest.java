package com.github.mike10004.socialapidemo;

import com.google.common.net.HostAndPort;
import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import twitter4j.Twitter;
import twitter4j.conf.ConfigurationBuilder;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertFalse;

public class TwitterCrawlerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public NanoHttpdRule httpdRule = new NanoHttpdRule();

    @Before
    public void setUp() throws Exception {
        Map<Predicate<? super NanoHTTPD.IHTTPSession>, String> presets = new HashMap<>();
        presets.put(session -> session.getUri().equals("/account/verify_credentials.json"),
                Tests.loadResourceAsString("/twitter/verify-credentials-1.json"));
        httpdRule.handle(session -> {
            return presets.entrySet().stream().filter(preset -> preset.getKey().test(session))
                    .map(preset -> NanoHttpdRule.respond(200, "application/json", preset.getValue()))
                    .findFirst().orElse(null);
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
        CrawlerConfig crawlerConfig = DirectConfig.builder().throttler(new TwitterThrottler())
                .assetProcessor(new FileStoringAssetProcessor(storageRoot.toPath(), new TwitterAssetSerializer()))
                .build();
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
