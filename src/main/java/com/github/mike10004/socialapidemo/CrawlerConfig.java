package com.github.mike10004.socialapidemo;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Abstract class for crawler dependency service factory. Concrete implementations
 * of this class instantiate services used by a crawler.
 */
public abstract class CrawlerConfig {

    private final LoadingCache<String, Object> serviceCache;

    protected CrawlerConfig() {
        serviceCache = CacheBuilder.newBuilder().build(new CacheLoader<String, Object>(){
            @Override
            public Object load(String key) throws Exception {
                switch (key) {
                    case "throttler": return buildThrottler();
                    case "assetProcessor": return buildAssetProcessor();
                    case "errorReactor": return buildErrorReactor();
                    case "visitRecorder": return buildVisitRecorder();
                    default:
                        throw new IllegalArgumentException("unknown service: " + key);
                }
            }
        });
    }

    public Throttler getThrottler() {
        return (Throttler) serviceCache.getUnchecked("throttler");
    }

    public AssetProcessor getAssetProcessor() {
        return (AssetProcessor) serviceCache.getUnchecked("assetProcessor");
    }

    public ErrorReactor getErrorReactor() {
        return (ErrorReactor) serviceCache.getUnchecked("errorReactor");
    }

    public VisitRecorder getVisitRecorder() {
        return (VisitRecorder) serviceCache.getUnchecked("visitRecorder");
    }

    protected Throttler buildThrottler() {
        return Throttler.inactive();
    }

    protected AssetProcessor buildAssetProcessor() {
        return AssetProcessor.logging();
    }

    protected ErrorReactor buildErrorReactor() {
        return ErrorReactor.rethrower();
    }

    public int getQueueCapacity() {
        return 8192;
    }

    protected VisitRecorder buildVisitRecorder() {
        return VisitRecorder.inMemory();
    }

    public Sleeper getSleeper() {
        return Sleeper.system();
    }

    public long getAssetCountLimit() {
        return Long.MAX_VALUE;
    }
}
