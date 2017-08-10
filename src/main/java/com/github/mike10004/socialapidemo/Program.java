package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.net.HostAndPort;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import facebook4j.Facebook;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import twitter4j.Twitter;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;

public class Program {

    private final BiFunction<Sns, OauthConfig, Object> clientFactory;

    public Program(BiFunction<Sns, OauthConfig, Object> clientFactory) {
        this.clientFactory = clientFactory;
    }

    public Program() {
        this(Sns::buildClient);
    }

    public static void main(String[] args) throws Exception {
        int exitcode = new Program().main0(args);
        System.exit(exitcode);
    }

    private static CharSink stdoutSink() {
        return new CharSink() {
            @Override
            public Writer openStream() throws IOException {
                return new OutputStreamWriter(new FilterOutputStream(System.out), StandardCharsets.UTF_8) {
                    @Override
                    public void close() throws IOException {
                    }
                };
            }
        };
    }

    private static final String DEFAULT_CONFIG_FILENAME = "social-load-config.json";

    static void logVerbosely(PrintStream output) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("com.github.mike10004");
        logger.setUseParentHandlers(false);
        Level level = Level.ALL;
        ConsoleHandler handler = new ConsoleHandler() {{
            setOutputStream(output);
        }};
        handler.setLevel(level);
        logger.setLevel(Level.FINEST);
        logger.addHandler(handler);
    }

    /**
     * Executes the program.
     * @param args array of arguments in syntax {@code PROXY MODE SNS},
     *             for example {@code localhost:36361 crawl twitter}
     * @return exit code
     */
    protected int main0(String...args) throws Exception {
        OptionParser parser = new OptionParser();
        File defaultConfigSourceFile = new File(System.getProperty("user.dir"), DEFAULT_CONFIG_FILENAME);
        parser.accepts("debug", "log verbosely on stdout");
        parser.acceptsAll(Arrays.asList("f", "config-file"), "config file; default is $PWD/" + DEFAULT_CONFIG_FILENAME).withRequiredArg().ofType(File.class).defaultsTo(defaultConfigSourceFile).describedAs("FILE");
        parser.accepts("proxy-type").withRequiredArg().ofType(Proxy.Type.class).defaultsTo(Proxy.Type.SOCKS).describedAs("TYPE");
        parser.accepts("port", "port to use for authorization redirect callback URL; required in authorization mode").withRequiredArg().ofType(Integer.class);
        parser.accepts("print-config-template", "print a config file template and exit");
        parser.accepts("redirect-path", "set path of redirect URI (default is empty path '/')").withRequiredArg().ofType(String.class).describedAs("PATH");
        parser.accepts("print-config", "print config after parsing");
        parser.accepts(OptionsConfig.OPT_THROTTLE, "set throttle strategy").withRequiredArg().ofType(OptionsConfig.ThrottleStrategy.class).describedAs("STRATEGY").defaultsTo(OptionsConfig.ThrottleStrategy.SNS_API_DEFAULT);
        parser.accepts(OptionsConfig.OPT_PROCESSOR, "set asset processor; URI with file:// scheme means store data in specified directory").withRequiredArg().ofType(String.class).describedAs("URI");
        parser.accepts(OptionsConfig.OPT_MAX_ERRORS, "tolerate MAX errors before aborting").withRequiredArg().ofType(Integer.class).describedAs("MAX");
        parser.accepts(OptionsConfig.OPT_MAX_ASSETS, "terminate after MAX assets processed").withRequiredArg().ofType(Long.class).describedAs("MAX");
        parser.accepts(OptionsConfig.OPT_QUEUE_CAPACITY, "set capacity of crawl node queue").withRequiredArg().ofType(Integer.class).describedAs("CAPACITY");
        OptionSet options = parser.parse(args);
        if (options.has("debug")) {
            logVerbosely(System.out);
        }
        if (options.has("print-config-template")) {
            ProgramConfig config = new ProgramConfig();
            config.oauthClients = new HashMap<>();
            config.oauthClients.put(Sns.twitter, new OauthCredentials("TWITTER_CLIENT_ID", "TWITTER_CLIENT_SECRET", new AccessBadge("TWITTER_ACCESS_TOKEN", "TWITTER_ACCESS_TOKEN_SECRET", null)));
            config.oauthClients.put(Sns.facebook, new OauthCredentials("FACEBOOK_CLIENT_ID", "FACEBOOK_CLIENT_SECRET", new AccessBadge("FACEBOOK_ACCESS_TOKEN", null, null)));
            saveConfig(config, stdoutSink());
            System.out.flush();
            return 0;
        }
        @SuppressWarnings("unchecked")
        List<String> positionals = (List<String>) options.nonOptionArguments();
        if (positionals.size() < 1) {
            System.err.println("first argument must be PROXY: host:port or 'DIRECT'");
            return 1;
        }
        String proxyAddressStr = positionals.get(0);
        HostAndPort proxyAddress = null;
        if (!"DIRECT".equalsIgnoreCase(proxyAddressStr)) {
            proxyAddress = HostAndPort.fromString(proxyAddressStr);
        }
        Proxy.Type proxyType = (Proxy.Type) options.valueOf("proxy-type");
        configureSystemProxy(proxyType, proxyAddress, System.getProperties());
        if (positionals.size() < 2) {
            System.err.println("first argument must be MODE: one of " + Arrays.toString(Mode.values()));
            return 1;
        }
        Mode mode = Mode.valueOf(positionals.get(1).toLowerCase());
        if (positionals.size() < 3) {
            System.err.println("third argument must be SNS: one of " + Arrays.toString(Sns.values()));
        }
        Sns sns = Sns.valueOf(positionals.get(2).toLowerCase());
        File configFile = (File) options.valueOf("config-file");
        CharSource configSource = CharSource.wrap(new Gson().toJson(new ProgramConfig()));
        if (configFile.exists()) {
            configSource = Files.asCharSource(configFile, StandardCharsets.UTF_8);
        }
        ProgramConfig config;
        try (Reader reader = configSource.openStream()) {
            config = new Gson().fromJson(reader, ProgramConfig.class);
        }
        if (options.has("print-config")) {
            saveConfig(config, stdoutSink());
        }
        PerformerFactory factory = getPerformerFactory(sns, config);
        HttpGetter getter = factory.getChecker();
        HttpCheckResult result = doHttpCheck(getter, proxyAddress);
        if (result != HttpCheckResult.OK) {
            return ERR_PROXY_CONFLICT;
        }
        if (mode == Mode.check && proxyAddress != null) {
            System.out.format("proxy usage confirmed: %s%n", proxyAddress);
        }
        switch (mode) {
            case check:
                // do nothing more; check already performed
                break;
            case authorize:
                Authmaster<?> authmaster = factory.getAuthmaster();
                @Nullable Integer port = (Integer) options.valueOf("port");
                if (port == null) {
                    System.err.println("--port must be specified if mode is " + Mode.authorize);
                    return 1;
                }
                @Nullable String redirectUriPath = (String) options.valueOf("redirect-path");
                config.oauthClients.get(sns).badge = authmaster.authorize(port, redirectUriPath);
                saveConfig(config, configFile);
                break;
            case demo:
                Demonstrator<?> demo = factory.getDemonstrator();
                demo.demonstrate();
                break;
            case crawl:
                CrawlerConfig crawlerConfig = createCrawlerConfig(sns, options);
                Crawler<?, ?> crawler = factory.getCrawler(crawlerConfig);
                crawler.crawl();
                break;
            default:
                throw new IllegalStateException("mode: " + mode);
        }
        return 0;
    }

    protected CrawlerConfig createCrawlerConfig(Program.Sns sns, OptionSet options) {
        return new OptionsConfig(sns, options);
    }

    protected void configureSystemProxy(Proxy.Type proxyType, @Nullable HostAndPort proxyAddress, Properties systemProperties) {
        if (proxyAddress != null) {
            switch (proxyType) {
                case HTTP:
                    setAll(systemProperties, Arrays.asList(SYSPROP_HTTP_PROXY_HOST, SYSPROP_HTTPS_PROXY_HOST), proxyAddress.getHost());
                    setAll(systemProperties, Arrays.asList(SYSPROP_HTTP_PROXY_PORT, SYSPROP_HTTPS_PROXY_PORT), String.valueOf(proxyAddress.getPort()));
                    systemProperties.setProperty(SYSPROP_HTTP_NON_PROXY_HOSTS, DEFAULT_NON_PROXY_HOSTS);
                    break;
                case SOCKS:
                    systemProperties.setProperty(SYSPROP_SOCKS_PROXY_HOST, proxyAddress.getHost());
                    systemProperties.setProperty(SYSPROP_SOCKS_PROXY_PORT, String.valueOf(proxyAddress.getPort()));
                    break;
                case DIRECT:
                    // do nothing
            }
        }
    }

    private void saveConfig(ProgramConfig config, File configFile) throws IOException {
        saveConfig(config, Files.asCharSink(configFile, StandardCharsets.UTF_8));
    }

    private void saveConfig(ProgramConfig config, CharSink configSink) throws IOException {
        try (PrintWriter writer = new PrintWriter(configSink.openStream())) {
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(config);
            writer.println(json);
            writer.flush();
            if (writer.checkError()) {
                throw new IOException("writer encountered error");
            }
        }
    }

    private static final int ERR_PROXY_CONFLICT = 2;

    public enum Sns {
        twitter, facebook;

        /**
         * Builds client.
         */
        @SuppressWarnings("unchecked")
        public <T> T buildClient(OauthConfig oauthConfig) {
            @Nullable AccessBadge badge = null;
            if (oauthConfig instanceof OauthCredentials) {
                badge = ((OauthCredentials)oauthConfig).badge;
            }
            switch (this) {
                case facebook:
                    return (T) SnsClientBuilder.facebook().buildClient(oauthConfig, badge);
                case twitter:
                    return (T) SnsClientBuilder.twitter().buildClient(oauthConfig, badge);
                default:
                    throw new IllegalStateException("not handled: " + this);
            }
        }

    }

    public enum Mode {
        check, authorize, demo, crawl
    }

    static class ProgramConfigBase<T extends OauthConfig> {
        public Map<Sns, T> oauthClients;
    }

    private static class ProgramConfig extends ProgramConfigBase<OauthCredentials> {
        public ProgramConfig() {
            oauthClients = ImmutableMap.of(
                Program.Sns.twitter, new OauthCredentials(null, null, null),
                Program.Sns.facebook, new OauthCredentials(null, null, null));
        }
    }

    private PerformerFactory getPerformerFactory(Sns sns, ProgramConfig config) throws IOException {
        OauthConfig oauthConfig = checkNotNull(checkNotNull(config.oauthClients, "configuration error; oauthClients not present").get(sns), "no config for %s", sns);
        switch (sns) {
            case twitter:
                Twitter twitterClient = (Twitter) clientFactory.apply(Sns.twitter, oauthConfig);
                return new PerformerFactory() {
                    @Override
                    public HttpGetter getChecker() {
                        return new TwitterHttpGetter(twitterClient);
                    }

                    @Override
                    public Authmaster<?> getAuthmaster() {
                        return new TwitterAuthmaster(twitterClient);
                    }

                    @Override
                    public Crawler<?, ?> getCrawler(CrawlerConfig crawlerConfig) {
                        return new TwitterCrawler(twitterClient, crawlerConfig);
                    }

                    @Override
                    public Demonstrator<?> getDemonstrator() {
                        return new TwitterDemo(twitterClient);
                    }
                };
            case facebook:
                Facebook facebookClient = (Facebook) clientFactory.apply(Sns.facebook, oauthConfig);
                return new PerformerFactory() {
                    @Override
                    public HttpGetter getChecker() {
                        return new FacebookHttpGetter(facebookClient);
                    }

                    @Override
                    public Authmaster<?> getAuthmaster() {
                        return new FacebookAuthmaster(facebookClient);
                    }

                    @Override
                    public Crawler<?, ?> getCrawler(CrawlerConfig crawlerConfig) {
                        return new FacebookCrawler(facebookClient, crawlerConfig);
                    }

                    @Override
                    public Demonstrator<?> getDemonstrator() {
                        return new FacebookDemo(facebookClient);
                    }
                };
            default:
                throw new IllegalArgumentException("sns: " + sns);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private interface PerformerFactory {
        HttpGetter getChecker();
        Authmaster<?> getAuthmaster();
        Demonstrator<?> getDemonstrator();
        Crawler<?, ?> getCrawler(CrawlerConfig crawlerConfig);
    }

    private enum HttpCheckResult {
        OK, CONFLICT, INDETERMINATE
    }

    protected HttpCheckResult doHttpCheck(HttpGetter getter, @Nullable HostAndPort proxy) throws MalformedURLException, UnknownHostException {
        URL[] urls = {
                new URL("http://httpbin.org/get"),
                new URL("https://httpbin.org/get"),
        };
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            InetAddress expectedAddress = null;
            if (proxy != null) {
                expectedAddress = InetAddress.getByName(proxy.getHost());
            }
            for (URL url : urls) {
                HttpGetter.SimpleResponse response;
                try {
                    response = executorService.submit(() -> getter.executeGet(url)).get(5, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    System.err.println("failed to confirm proxy settings");
                    e.printStackTrace(System.err);
                    return HttpCheckResult.INDETERMINATE;
                }
                JsonObject responseObj = new JsonParser().parse(response.text).getAsJsonObject();
                String origin = responseObj.get("origin").getAsString();
                if (expectedAddress != null) {
                    if (!expectedAddress.getHostAddress().equals(origin)) {
                        System.err.format("expected %s but was %s%n", expectedAddress.getHostAddress(), origin);
                        return HttpCheckResult.CONFLICT;
                    }
                }
            }
        } finally {
            executorService.shutdown();
        }
        return HttpCheckResult.OK;
    }

    static final String SYSPROP_SOCKS_PROXY_HOST = "socksProxyHost";
    static final String SYSPROP_SOCKS_PROXY_PORT = "socksProxyPort";
    static final String SYSPROP_HTTP_PROXY_HOST = "http.proxyHost";
    static final String SYSPROP_HTTP_PROXY_PORT = "http.proxyPort";
    static final String SYSPROP_HTTPS_PROXY_HOST = "https.proxyHost";
    static final String SYSPROP_HTTPS_PROXY_PORT = "https.proxyPort";
//    static final String SYSPROP_HTTP_PROXY_USER = "http.proxyUser";
//    static final String SYSPROP_HTTP_PROXY_PASSWORD = "http.proxyPassword";
    static final String SYSPROP_HTTP_NON_PROXY_HOSTS = "http.nonProxyHosts";
    static final String DEFAULT_NON_PROXY_HOSTS = "localhost|127.*|[::1]";

    private static void setAll(Properties p, Iterable<String> keys, String value) {
        for (String key : keys) {
            p.setProperty(key, value);
        }
    }
}
