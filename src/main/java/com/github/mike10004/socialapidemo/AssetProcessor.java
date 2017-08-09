package com.github.mike10004.socialapidemo;

import com.google.common.collect.Lists;

import javax.annotation.Nullable;

public interface AssetProcessor {
    void process(Object asset, Iterable<String> lineage);
    default void process(Object asset, @Nullable String category, String...identifiers) {
        process(asset, Lists.asList(category, identifiers));
    }
}
