package com.github.mike10004.socialapidemo;

import com.google.common.base.MoreObjects;

public class DirectConfig extends CrawlerConfig {

    private final Throttler throttler;

    private final AssetProcessor assetProcessor;

    public DirectConfig(Throttler throttler, AssetProcessor assetProcessor) {
        this.throttler = throttler;
        this.assetProcessor = MoreObjects.firstNonNull(assetProcessor, AssetProcessor.logging());
    }

    @Override
    public Throttler buildThrottler() {
        return throttler;
    }

    @Override
    public AssetProcessor buildAssetProcessor() {
        return assetProcessor;
    }

}
