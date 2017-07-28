package com.github.mike10004.socialapidemo;

import com.google.gson.Gson;
import org.junit.Assume;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.ServerSocket;

public class Tests {

    private static final String EXAMPLE_CONFIG_FILENAME = "example-config.json";

    private Tests() {}

    public static int provideUsablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    public static class OauthClientConfig extends OauthClient {

        @Nullable
        public Integer redirectUriPort;

        public OauthClientConfig(String clientId, String clientSecret, @Nullable Integer port) {
            super(clientId, clientSecret);
            this.redirectUriPort = port;
        }
    }

    public static OauthClientConfig loadExampleOauthCreds(Program.Sns sns) throws IOException {
        File configFile = new File(System.getProperty("user.dir"), EXAMPLE_CONFIG_FILENAME);
        Assume.assumeTrue(EXAMPLE_CONFIG_FILENAME + " not found", configFile.isFile());
        try (Reader reader = new FileReader(configFile)) {
            TestConfig config = new Gson().fromJson(reader, TestConfig.class);
            Assume.assumeTrue("config null or empty", config != null && config.oauthClients != null);
            OauthClientConfig oauthClient = config.oauthClients.get(sns);
            Assume.assumeTrue("no creds for " + sns + " in " + config.oauthClients, oauthClient != null);
            return oauthClient;
        }
    }

    private static class TestConfig extends Program.ProgramConfigBase<OauthClientConfig> {
    }
}
