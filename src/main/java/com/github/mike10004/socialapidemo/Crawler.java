package com.github.mike10004.socialapidemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Social network service API crawler. A crawler helps to demonstrate
 * sequential browsing with processing (e.g. storage) of data encountered
 * while adjusting for API rate limits.
 * @param <C> client type
 */
public abstract class Crawler<C, X extends Exception> {

    protected final C client;
    protected final Throttler throttler;
    protected final AssetProcessor assetProcessor;
    protected final CrawlerConfig crawlerConfig;

    protected Crawler(C client, CrawlerConfig crawlerConfig, Throttler throttler, AssetProcessor assetProcessor) {
        this.client = checkNotNull(client);
        this.crawlerConfig = crawlerConfig;
        this.throttler = checkNotNull(throttler);
        this.assetProcessor = checkNotNull(assetProcessor);
    }

    public abstract void crawl() throws CrawlerException, X;

    protected <T> T throttle(@Nullable String category, CheckedCallable<T, X> callable) throws X {
        throttler.delay(category);
        try {
            return callable.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception exception) {
            try {
                //noinspection unchecked
                if (isRateLimitException((X) exception)) {
                    //noinspection unchecked
                    return getRateLimitReaction((X) exception).react(callable);
                }
            } catch (ClassCastException classCastException) {
                throw new AssertionError("expected different exception class from " + exception, classCastException);
            }
            //noinspection unchecked
            throw (X) exception;
        }
    }

    protected abstract RateLimitReaction<X> getRateLimitReaction(X exception);

    protected interface RateLimitReaction<X extends Exception> {
        <T> T react(CheckedCallable<T, X> callable) throws X;

        static <X extends Exception> RateLimitReaction<X> fail(X exception) {
            return new RateLimitReaction<X>() {
                @Override
                public <T> T react(CheckedCallable<T, X> callable) throws X {
                    throw exception;
                }
            };
        }
    }

    protected abstract boolean isRateLimitException(X exception);

    protected static class LoggingAssetProcessor implements AssetProcessor {

        private final Logger log = LoggerFactory.getLogger(getClass());

        @Override
        public void process(Object asset, Iterable<String> lineage) {
            log.debug("{}", lineage);
        }
    }

}
