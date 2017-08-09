package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.util.Collections;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Social network service API crawler. A crawler helps to demonstrate
 * sequential browsing with processing (e.g. storage) of data encountered
 * while adjusting for API rate limits.
 * @param <C> client type
 */
public abstract class Crawler<C, X extends Exception> {

    protected final C client;
    protected final CrawlerConfig crawlerConfig;

    protected Crawler(C client, CrawlerConfig crawlerConfig) {
        this.client = checkNotNull(client);
        this.crawlerConfig = checkNotNull(crawlerConfig);
    }

    public abstract void crawl() throws CrawlerException, X;

    public abstract class Action<T, E extends Exception> implements CheckedCallable<T, E> {

        @Nullable
        private final String category;

        @SuppressWarnings("unused")
        protected Action() {
            this(null);
        }

        protected Action(@Nullable String category) {
            this.category = category;
        }

        @Nullable
        public String getCategory() {
            return category;
        }
        public Iterable<String> getLineage(T asset) {
            return ImmutableList.of();
        }
    }

    protected Throttler getThrottler() {
        return crawlerConfig.getThrottler();
    }

    protected AssetProcessor getAssetProcessor() {
        return crawlerConfig.getAssetProcessor();
    }

    protected ErrorReactor getErrorReactor() {
        return crawlerConfig.getErrorReactor();
    }

    protected <T> void acquire(Action<T, X> action) throws X {
        String category = action.getCategory();
        getThrottler().delay(category);
        T asset = null;
        try {
            asset = action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception exception) {
            X typedException;
            try {
                //noinspection unchecked
                typedException = (X) exception; // enforced by Action.call sgnature
            } catch (ClassCastException classCastException) {
                throw new AssertionError("expected different exception class from " + exception, classCastException);
            }
            getErrorReactor().react(typedException);
        }
        if (asset != null) {
            getAssetProcessor().process(asset, Iterables.concat(Collections.singleton(category), action.getLineage(asset)));
        }
    }

}
