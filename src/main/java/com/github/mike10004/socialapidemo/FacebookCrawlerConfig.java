package com.github.mike10004.socialapidemo;

import joptsimple.OptionSet;

public class FacebookCrawlerConfig extends OptionsConfig {

    public FacebookCrawlerConfig(OptionSet options) {
        super(options);
    }

    @Override
    protected AssetSerializer buildAssetSerializer() {
        return new FacebookAssetSerializer();
    }

    @Override
    protected Throttler buildThrottler(ThrottleStrategy throttleStrategy) {
        switch (throttleStrategy) {
            case NONE:
                return Throttler.inactive();
            default:
                throw new UnsupportedOperationException("ThrottleStrategy." + throttleStrategy);
        }

    }
}
