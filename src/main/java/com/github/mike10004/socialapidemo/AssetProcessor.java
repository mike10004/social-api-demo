package com.github.mike10004.socialapidemo;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public interface AssetProcessor {
    void process(Object asset, Iterable<String> lineage);
    default void process(Object asset, @Nullable String category, String...identifiers) {
        process(asset, Lists.asList(category, identifiers));
    }

    static AssetProcessor logging() {
        return new AssetProcessor() {
            @Override
            public void process(Object asset, Iterable<String> lineage) {
                LoggerFactory.getLogger(getClass()).debug("{}", lineage);
            }
        };
    }

}
