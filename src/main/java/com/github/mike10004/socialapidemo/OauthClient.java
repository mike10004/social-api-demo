package com.github.mike10004.socialapidemo;

public class OauthClient {

    public final String clientId;
    public final String clientSecret;

    public OauthClient(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OauthClient that = (OauthClient) o;

        if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null) return false;
        return clientSecret != null ? clientSecret.equals(that.clientSecret) : that.clientSecret == null;
    }

    @Override
    public int hashCode() {
        int result = clientId != null ? clientId.hashCode() : 0;
        result = 31 * result + (clientSecret != null ? clientSecret.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "OauthClient{" +
                "clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                '}';
    }
}
