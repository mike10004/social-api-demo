package com.github.mike10004.socialapidemo;

import javax.annotation.Nullable;

public class TwitterCrawlerStrategy {
    @Nullable
    public final String firstUserId;

    public TwitterCrawlerStrategy() {
        this(null);
    }

    public TwitterCrawlerStrategy(@Nullable String firstUserId) {
        this.firstUserId = firstUserId;
    }
}
