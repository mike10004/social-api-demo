package com.github.mike10004.socialapidemo;

import com.google.common.base.MoreObjects;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class OptionsConfig extends CrawlerConfig {

    private static final Logger log = LoggerFactory.getLogger(OptionsConfig.class);

    public static final String OPT_THROTTLE = "throttle";
    public static final String OPT_PROCESSOR = "processor";
    public static final String OPT_MAX_ERRORS = "max-errors";
    public static final String OPT_MAX_ASSETS = "max-assets";
    public static final String OPT_QUEUE_CAPACITY = "queue-capacity";
    public static final String OPT_SEED = "seed";

    private OptionSet options;

    @SuppressWarnings("unused")
    public enum ThrottleStrategy {
        NONE, SNS_API_DEFAULT, SNS_API_GREEDY
    }

    protected OptionsConfig(OptionSet options) {
        this.options = options;
    }

    @Override
    public Throttler buildThrottler() {
        ThrottleStrategy throttleStrategy = (ThrottleStrategy) checkNotNull(options.valueOf(OPT_THROTTLE), "throttle strategy");
        Throttler throttler = buildThrottler(throttleStrategy);
        log.debug("throttle strategy {} yields {}", throttleStrategy, throttler);
        return throttler;
    }

    protected abstract AssetSerializer buildAssetSerializer();

    protected abstract Throttler buildThrottler(ThrottleStrategy throttleStrategy);

    @Override
    public AssetProcessor buildAssetProcessor() {
        String storageSpecUriStr = (String) options.valueOf(OPT_PROCESSOR);
        if (storageSpecUriStr != null) {
            URI storageSpecUri = URI.create(storageSpecUriStr);
            if ("file".equals(storageSpecUri.getScheme())) {
                File root = new File(storageSpecUri);
                AssetSerializer serializer = buildAssetSerializer();
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

    private <N extends Number> N maybeGet(String flag, N defaultValue) {
        checkNotNull(defaultValue, "must provide non-null default for %s", flag);
        @SuppressWarnings("unchecked")
        @Nullable N value = (N) options.valueOf(flag);
        return MoreObjects.firstNonNull(value, defaultValue);
    }

    @Override
    public int getQueueCapacity() {
        return maybeGet(OPT_QUEUE_CAPACITY, super.getQueueCapacity());
    }

    @Override
    public long getAssetCountLimit() {
        return maybeGet(OPT_MAX_ASSETS, super.getAssetCountLimit());
    }

    @Nullable
    @Override
    public String getSeedSpecification() {
        @Nullable String seed = (String) options.valueOf(OPT_SEED);
        return seed;
    }

}
