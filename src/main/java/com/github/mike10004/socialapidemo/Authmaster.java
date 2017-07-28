package com.github.mike10004.socialapidemo;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.net.MediaType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class Authmaster<S extends OauthState> {

    private static final String MIME_TYPE_JSON = MediaType.JSON_UTF_8.withoutParameters().toString();
    private static final Logger log = LoggerFactory.getLogger(Authmaster.class);

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, S> states;

    protected Authmaster() {
        states = new HashMap<>();
    }

    protected String generateStateKey() {
        return CharMatcher.javaLetterOrDigit().retainFrom(UUID.randomUUID().toString());
    }

    public AccessBadge authorize(int redirectPort) throws IOException, AuthmasterException, InterruptedException {
        return authorize(redirectPort, null);
    }

    public AccessBadge authorize(int redirectPort, @Nullable String redirectUrlPath) throws IOException, AuthmasterException, InterruptedException {
        S stateValue = createOauthState(constructRedirectUrl(redirectPort, redirectUrlPath));
        return doAuthorize(redirectPort, stateValue);
    }

    public URL constructRedirectUrl(int redirectPort, @Nullable String path) throws MalformedURLException {
        String base = "http://localhost:" + redirectPort + "/";
        URIBuilder builder = new URIBuilder(base);
        if (path != null) {
            builder.setPath(path);
        }
        return builder.build().toURL();
    }

    protected AccessBadge doAuthorize(int redirectPort, S stateValue) throws IOException, AuthmasterException, InterruptedException {
        states.put(stateValue.getKey(), stateValue);
        System.out.format("Visit URL to authorize:%n%s%n", stateValue.getAuthorizationUrl());
        return useRedirectServer(redirectPort);
    }

    @SuppressWarnings("unused")
    public S createOauthState(int redirectUriPort) throws MalformedURLException {
        return createOauthState(redirectUriPort, null);
    }

    public S createOauthState(int redirectUriPort, String path) throws MalformedURLException {
        return createOauthState(constructRedirectUrl(redirectUriPort, path));
    }

    public S createOauthState(URL redirectUri) {
        return createOauthState(redirectUri, generateStateKey());
    }

    public abstract S createOauthState(URL redirectUri, String stateKey);

    protected AccessBadge exchangeCodeForAccessToken(URI uri, MultiMap<String> query, Map<String, S> states) {
        String stateKey = getStateFromRedirect(query);
        S stateValue = states.get(stateKey);
        if (stateValue == null) {
            throw new StateOrCodeNotPresentException("state not in states map: " + stateKey);
        }
        states.remove(stateKey);
        return exchangeCodeForAccessToken(uri, query, stateValue);
    }

    protected String getStateFromRedirect(MultiMap<String> query) {
        String stateKey = query.getValue("state", 0);
        if (stateKey == null) {
            throw new AuthmasterException("state not in query " + query);
        }
        return stateKey;
    }

    @SuppressWarnings("unused")
    protected AccessBadge exchangeCodeForAccessToken(URI uri, MultiMap<String> query, S state) {
        String code = getCode(query);
        if (code == null) {
            throw new StateOrCodeNotPresentException("code not present in " + query);
        }
        return exchangeCodeForAccessToken(state, code);
    }

    @Nullable
    protected String getCode(MultiMap<String> query) {
        return query.getValue(getCodeParamName(), 0);
    }

    protected abstract String getCodeParamName();

    protected abstract AccessBadge exchangeCodeForAccessToken(S state, String code);

    protected void redirectServerStarted() {}

    private AccessBadge useRedirectServer(int port) throws IOException, InterruptedException {
        Server server = new Server(port);
        RedirectHandler handler = new RedirectHandler();
        server.setHandler(handler);
        try {
            server.start();
            System.out.format("listening for redirect at http://localhost:%d/%n", port);
            redirectServerStarted();
            log.trace("started redirect server");
        } catch (Exception e) {
            throw new AuthmasterException("server start error", e);
        }
        try {
            return handler.waitForResponse();
        } finally {
            try {
                server.stop();
                server.join();
                log.debug("server stop() and join() invoked");
            } catch (Throwable e) {
                log.warn("error stopping or joining server", e);
            }
        }
    }

    private class RedirectHandler extends AbstractHandler {

        private final BlockingQueue<AccessBadge> responseQueue;

        public RedirectHandler() {
            responseQueue = new ArrayBlockingQueue<>(1024);
        }

        @Override
        public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
            if ("/favicon.ico".equals(request.getPathInfo())) {
                httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            System.out.format("%s %s %s%n", request.getMethod(), request.getPathInfo(), Strings.nullToEmpty(request.getQueryString()));
            HttpURI uri = request.getUri();
            try {
                request.extractParameters();
                AccessBadge accessBadge = exchangeCodeForAccessToken(URI.create(uri.toString()), request.getQueryParameters(), states);
                httpServletResponse.setContentType(MIME_TYPE_JSON);
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                Writer writer = httpServletResponse.getWriter();
                gson.toJson(accessBadge, writer);
                writer.flush();
                writer.close();
                responseQueue.add(accessBadge); // throws exception if at capacity already
            } catch (StateOrCodeNotPresentException e) {
                log.debug("responding 404 to {} due to {}", request, e.toString());
                httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (AuthmasterException e) {
                log.info("responding 500 due to processing failure on " + request, e);
                httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        public AccessBadge waitForResponse() throws InterruptedException {
            return responseQueue.take(); // blocks until something is available to return
        }
    }

    @SuppressWarnings("unused")
    public static class AuthmasterException extends ExerciseException {
        public AuthmasterException(String message) {
            super(message);
        }

        public AuthmasterException(String message, Throwable cause) {
            super(message, cause);
        }

        public AuthmasterException(Throwable cause) {
            super(cause);
        }
    }

    public static class StateOrCodeNotPresentException extends AuthmasterException {
        public StateOrCodeNotPresentException(String message) {
            super(message);
        }
    }
}
