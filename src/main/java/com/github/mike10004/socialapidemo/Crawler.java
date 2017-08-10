package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Social network service API crawler. A crawler helps to demonstrate
 * sequential browsing with processing (e.g. storage) of data encountered
 * while adjusting for API rate limits.
 * @param <C> client type
 */
public abstract class Crawler<C, X extends Exception> {

    private static final Logger log = LoggerFactory.getLogger(Crawler.class);

    protected final C client;
    protected final CrawlerConfig crawlerConfig;

    protected Crawler(C client, CrawlerConfig crawlerConfig) {
        this.client = checkNotNull(client);
        this.crawlerConfig = checkNotNull(crawlerConfig);
    }

    /**
     * Starts a crawl. Plucks a seed from the seed generator, performs
     * its action, and queues up subsequent actions. Proceeds in this
     * breadth-first manner until the queue is exhausted, then moves
     * to the next seed.
     */
    public final void crawl() throws CrawlerException, X {
        crawl(crawlerConfig.getQueueCapacity());
    }

    protected final void crawl(int queueCapacity) throws CrawlerException, X {
        Iterator<Action<?, X>> seeds = getSeedGenerator();
        BlockingQueue<Action<?, X>> queue = new ArrayBlockingQueue<>(queueCapacity);
        while (seeds.hasNext()) {
            Action<?, X> seed = seeds.next();
            queue.add(seed);
            while (!queue.isEmpty()) {
                Action<?, X> target = queue.remove();
                Iterable<Action<?, X>> branches = acquire(target);
                branches.forEach(queue::offer);
            }
            log.debug("action queue exhausted; proceeding to next seed");
        }
        log.debug("seeds exhausted; terminating crawl");
    }

    protected Iterator<Action<?, X>> getSeedGenerator() {
        return ImmutableList.<Action<?, X>>of().iterator();
    }

    public abstract static class Action<T, E extends Exception> implements CheckedCallable<T, E> {

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

        public Iterable<Action<?, E>> findNextTargets(T asset) throws E {
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

    protected <T> Iterable<Action<?, X>> acquire(Action<T, X> action) throws X {
        String category = action.getCategory();
        getThrottler().delay(category);
        T asset;
        try {
            asset = action.call();
            if (asset != null) {
                getAssetProcessor().process(asset, Iterables.concat(Collections.singleton(category), action.getLineage(asset)));
                return action.findNextTargets(asset);
            }
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
        return ImmutableList.of();
    }

}
