package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableMap;
import com.google.gson.annotations.JsonAdapter;

import javax.annotation.Nullable;

import java.time.Instant;
import java.util.Map;

public class AccessBadge {

    public final String accessToken;

    @Nullable
    public final String accessSecret;

    @Nullable
    @JsonAdapter(IsoFormatInstantTypeAdapter.class)
    public final Instant expiry;

    @Nullable
    private final Map<String, String> extras;

    public AccessBadge(String accessToken, String accessSecret, @Nullable Instant expiry) {
        this(accessToken, accessSecret, expiry, null);
    }

    public AccessBadge(String accessToken, @Nullable String accessSecret, @Nullable Instant expiry, Map<String, String> extras) {
        this.accessToken = accessToken;
        this.accessSecret = accessSecret;
        this.expiry = expiry;
        this.extras = (extras == null || extras.isEmpty()) ? null : ImmutableMap.copyOf(extras);
    }

    @Override
    public String toString() {
        return "AccessBadge{" +
                "accessToken='" + accessToken + '\'' +
                ", accessSecret='" + accessSecret + '\'' +
                ", expiry=" + expiry +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccessBadge that = (AccessBadge) o;

        if (accessToken != null ? !accessToken.equals(that.accessToken) : that.accessToken != null) return false;
        if (accessSecret != null ? !accessSecret.equals(that.accessSecret) : that.accessSecret != null) return false;
        return expiry != null ? expiry.equals(that.expiry) : that.expiry == null;
    }

    @Override
    public int hashCode() {
        int result = accessToken != null ? accessToken.hashCode() : 0;
        result = 31 * result + (accessSecret != null ? accessSecret.hashCode() : 0);
        result = 31 * result + (expiry != null ? expiry.hashCode() : 0);
        result = 31 * result + (extras != null ? extras.hashCode() : 0);
        return result;
    }

    public String getExtra(String key) {
        return extras == null ? null : extras.get(key);
    }
}
