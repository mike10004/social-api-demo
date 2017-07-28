package com.github.mike10004.socialapidemo;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class AuthmasterTest {

    @org.junit.Test
    public void authorize() throws Exception {
        LoggerFactory.getLogger(getClass()).trace("authorize() test started");
        String code = "12345";
        int port = Tests.provideUsablePort();
        AtomicReference<String> stateKey = new AtomicReference<>();
        final Object serverStartWatch = new Object();
        Authmaster<?> authmaster = new TestAuthmaster() {
            @Override
            protected String generateStateKey() {
                String generated = super.generateStateKey();
                checkState(stateKey.compareAndSet(null, generated));
                return generated;
            }

            @Override
            protected void redirectServerStarted() {
                synchronized (serverStartWatch) {
                    serverStartWatch.notifyAll();
                }
            }
        };
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<AccessBadge> badgeFuture = executor.submit(() -> authmaster.authorize(port)); // blocks until request received
        synchronized (serverStartWatch) {
            serverStartWatch.wait();
        }
        URL url = new URIBuilder("http://localhost:" + port + "/")
                .addParameter("code", code)
                .addParameter("state", stateKey.get())
                .build().toURL();
        try (InputStream http = url.openStream()) {
            String responseText = new String(ByteStreams.toByteArray(http), UTF_8);
            System.out.format("response text: " + responseText);
            System.out.println();
        }
        String expectedToken = codeToToken(code);
        AccessBadge badge = badgeFuture.get(5, TimeUnit.SECONDS);
        System.out.println("returned badge: " + badge);
        assertEquals("badge token", expectedToken, badge.accessToken);
    }

    private static String codeToToken(String code) {
        return Hashing.sha256().hashString(code, UTF_8).toString();
    }

    private static class TestAuthmaster extends Authmaster<OauthState> {

        @Override
        public OauthState createOauthState(URL redirectUri, String stateKey) {
            URL url;
            try {
                url = new URIBuilder(redirectUri.toString()).addParameter("state", stateKey).build().toURL();
            } catch (MalformedURLException e) {
                throw new AuthmasterException(e);
            }
            return new OauthState() {
                @Override
                public String getKey() {
                    return stateKey;
                }

                @Override
                public URL getAuthorizationUrl() {
                    return url;
                }
            };
        }

        @Override
        protected String getCodeParamName() {
            return "code";
        }

        @Override
        protected AccessBadge exchangeCodeForAccessToken(OauthState state, String code) {
            String accessToken = codeToToken(code);
            return new AccessBadge(accessToken, null, null);
        }

    }
}