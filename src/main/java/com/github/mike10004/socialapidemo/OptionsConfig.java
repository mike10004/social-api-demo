package com.github.mike10004.socialapidemo;

import joptsimple.OptionSet;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URI;

import static com.google.common.base.Preconditions.checkState;

public class OptionsConfig extends CrawlerConfig {

    public static final String OPT_THROTTLE = "throttle";
    public static final String OPT_PROCESSOR = "processor";
    public static final String OPT_MAX_ERRORS = "max-errors";

    private Program.Sns sns;
    private OptionSet options;

    @SuppressWarnings("unused")
    public enum ThrottleStrategy {
        NONE, SNS_API_DEFAULT
    }

    public OptionsConfig(Program.Sns sns, OptionSet options) {
        this.sns = sns;
        this.options = options;
    }

    @Override
    public Throttler buildThrottler() {
        ThrottleStrategy throttleStrategy = (ThrottleStrategy) options.valueOf(OPT_THROTTLE);
        checkState(throttleStrategy != null, "no throttle strategy defined");
        switch (throttleStrategy) {
            case NONE:
                return Throttler.inactive();
        }
        checkState(throttleStrategy == ThrottleStrategy.SNS_API_DEFAULT);
        switch (sns) {
            case twitter:
                return new TwitterThrottler();
            default:
                throw new IllegalStateException("no default throttle strategy for " + sns);
        }
    }

    @Override
    public AssetProcessor buildAssetProcessor() {
        String storageSpecUriStr = (String) options.valueOf(OPT_PROCESSOR);
        if (storageSpecUriStr != null) {
            URI storageSpecUri = URI.create(storageSpecUriStr);
            if ("file".equals(storageSpecUri.getScheme())) {
                File root = new File(storageSpecUri);
                AssetSerializer serializer;
                switch (sns) {
                    case twitter:
                        serializer = new TwitterAssetSerializer();
                        break;
                    default:
                        throw new IllegalStateException("no asset serializer defined for " + sns);
                }
                return new FileStoringAssetProcessor(root.toPath(), serializer);
            }
            throw new IllegalStateException("not handled: " + storageSpecUriStr);
        }
        return super.buildAssetProcessor();
    }

    @Override
    protected ErrorReactor buildErrorReactor() {
        @Nullable Integer maxErrors = (Integer) options.valueOf(OPT_MAX_ERRORS);
        if (maxErrors != null) {
            return ErrorReactor.limiter(maxErrors);
        }
        return super.buildErrorReactor();
    }
}
