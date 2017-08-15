package com.github.mike10004.socialapidemo;

import joptsimple.OptionSet;

public class TwitterCrawlerConfig extends OptionsConfig {

    public TwitterCrawlerConfig(OptionSet options) {
        super(options);
    }

    @Override
    protected AssetSerializer buildAssetSerializer() {
        return new TwitterAssetSerializer();
    }

    @Override
    protected Throttler buildThrottler(ThrottleStrategy throttleStrategy) {
        switch (throttleStrategy) {
            case SNS_API_DEFAULT:
                return TwitterThrottler.steady();
            case SNS_API_GREEDY:
                return TwitterThrottler.greedy();
            case NONE:
                return Throttler.inactive();
            default:
                throw new IllegalStateException("unhandled: ThrottleStrategy." + throttleStrategy);
        }
    }
}
