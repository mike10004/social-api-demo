package com.github.mike10004.socialapidemo;

import javax.annotation.Nullable;

public class DirectConfig extends CrawlerConfig {

    @Nullable
    private final Throttler throttler;

    @Nullable
    private final AssetProcessor assetProcessor;

    @Nullable
    private final ErrorReactor errorReactor;

    public DirectConfig() {
        this(builder());
    }

    private DirectConfig(Builder builder) {
        throttler = builder.throttler;
        assetProcessor = builder.assetProcessor;
        errorReactor = builder.errorReactor;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Throttler buildThrottler() {
        return throttler;
    }

    @Override
    public AssetProcessor buildAssetProcessor() {
        return assetProcessor;
    }

    @Override
    protected ErrorReactor buildErrorReactor() {
        return errorReactor;
    }


    public static final class Builder {
        private Throttler throttler = Throttler.inactive();
        private AssetProcessor assetProcessor = AssetProcessor.logging();
        private ErrorReactor errorReactor = ErrorReactor.rethrower();

        private Builder() {
        }

        public Builder throttler(Throttler throttler) {
            this.throttler = throttler;
            return this;
        }

        public Builder assetProcessor(AssetProcessor assetProcessor) {
            this.assetProcessor = assetProcessor;
            return this;
        }

        public Builder errorReactor(ErrorReactor errorReactor) {
            this.errorReactor = errorReactor;
            return this;
        }

        public DirectConfig build() {
            return new DirectConfig(this);
        }
    }
}
