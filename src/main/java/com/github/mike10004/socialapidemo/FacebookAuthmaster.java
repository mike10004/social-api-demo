package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.internal.http.HttpClientWrapper;
import facebook4j.internal.http.HttpResponse;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class FacebookAuthmaster extends Authmaster<FacebookOauthState> {

    private final Facebook facebookClient;

    public FacebookAuthmaster(Facebook facebookClient) {
        this.facebookClient = facebookClient;
    }

    @Override
    public FacebookOauthState createOauthState(URL redirectUri, String stateKey) {
        URL authorizationUrl;
        try {
            authorizationUrl = new URL(facebookClient.getOAuthAuthorizationURL(redirectUri.toString(), stateKey));
            return new FacebookOauthState(stateKey, authorizationUrl, redirectUri.toURI());
        } catch (MalformedURLException | URISyntaxException e) {
            throw new AuthmasterException(e);
        }

    }

    @Override
    protected String getCodeParamName() {
        return "code";
    }

    @Override
    protected AccessBadge exchangeCodeForAccessToken(FacebookOauthState state, String oauthCode) {
        TokenResponse accessTokenResponse = exchangeAccessToken(facebookClient, oauthCode, state.getRedirectUri().toString());
        Long secondsUntilExpiration = accessTokenResponse.expires_in;
        if (secondsUntilExpiration == null) {
            secondsUntilExpiration = PRESUMED_ACCESS_TOKEN_VALIDITY_DURATION.getSeconds();
        }
        Instant expiry = Instant.now().plus(Duration.ofSeconds(secondsUntilExpiration));
        Map<String, String> extras = mapOfNonNullValues("accessTokenAuthNonce", accessTokenResponse.auth_nonce,
            "authType", accessTokenResponse.auth_type,
            "accessTokenType", accessTokenResponse.token_type);
        AccessBadge badge = new AccessBadge(accessTokenResponse.access_token, null, expiry, extras);
        return badge;
    }

    private static ImmutableMap<String, String> mapOfNonNullValues(String...keyValuePairs) {
        ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            String key = keyValuePairs[i], value = keyValuePairs[i + 1];
            if (value != null) {
                b.put(key, value);
            }
        }
        return b.build();
    }

    public static final Duration PRESUMED_ACCESS_TOKEN_VALIDITY_DURATION = Duration.ofDays(30);

    protected TokenResponse exchangeAccessToken(Facebook facebook, String oauthCode, String redirectUri) {
        if (oauthCode == null || redirectUri == null) {
            throw new AuthmasterException("oauth code and redirect URI must be non-null to exchange code for access token");
        }
        try {
            String exchangeAccessTokenUrl = buildExchangeAccessTokenURL(facebook, redirectUri, oauthCode);
            return sendExchangeAccessTokenRequest(facebook, exchangeAccessTokenUrl);
        } catch (FacebookException ex) {
            throw new AuthmasterException(ex);
        }
    }

    protected String buildExchangeAccessTokenURL(Facebook facebook, String redirectUrl, String oauthCode) {
        String clientId = checkNotNull(facebook.getConfiguration().getOAuthAppId(), "client id not present in facebook client configuration");
        String clientSecret = checkNotNull(facebook.getConfiguration().getOAuthAppSecret(), "client secret not present in facebook client configuration");
        String oauthAccessTokenUrl = facebook.getConfiguration().getOAuthAccessTokenURL();
        return oauthAccessTokenUrl +
                "?client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&redirect_uri=" + redirectUrl +
                "&code=" + oauthCode;
    }

    private static class TokenResponse {

        public final String access_token;
        public final String token_type;
        public final String auth_nonce;
        public final String auth_type;
        public final Long expires_in;

        public TokenResponse(String access_token, String token_type, String auth_nonce, String auth_type, Long expires_in) {
            this.access_token = access_token;
            this.token_type = token_type;
            this.auth_nonce = auth_nonce;
            this.auth_type = auth_type;
            this.expires_in = expires_in;
        }
    }

    protected TokenResponse sendExchangeAccessTokenRequest(Facebook facebook, String exchangeAccessTokenUrl) throws FacebookException {
        HttpClientWrapper http = new HttpClientWrapper(facebook.getConfiguration());
        HttpResponse response = http.get(exchangeAccessTokenUrl);
        if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
            String json = response.asString();
            return new Gson().fromJson(json, TokenResponse.class);
        } else {
            throw new AuthmasterException("status " + response.getStatusCode() + " in response to access token exchange request");
        }
    }
}
