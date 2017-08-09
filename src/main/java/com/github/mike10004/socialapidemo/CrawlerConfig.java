package com.github.mike10004.socialapidemo;

public class CrawlerConfig {

    /**
     * Asset processor specification URI. Individual asset processor
     * implementations are selected and configured based on the URI.
     */
    public final String processorSpecUri;

    /**
     * Strategy to use for throttling API activity.
     */
    public final ThrottleStrategy throttleStrategy;

    private CrawlerConfig(Builder builder) {
        processorSpecUri = builder.processorSpecUri;
        throttleStrategy = builder.throttleStrategy;
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("unused")
    public enum ThrottleStrategy {
        NONE, SNS_API_DEFAULT
    }


    public static final class Builder {
        private String processorSpecUri;
        private ThrottleStrategy throttleStrategy;

        private Builder() {
        }

        public Builder processorSpecUri(String processorSpecUri) {
            this.processorSpecUri = processorSpecUri;
            return this;
        }

        public Builder throttleStrategy(ThrottleStrategy throttleStrategy) {
            this.throttleStrategy = throttleStrategy;
            return this;
        }

        public CrawlerConfig build() {
            return new CrawlerConfig(this);
        }
    }
}
