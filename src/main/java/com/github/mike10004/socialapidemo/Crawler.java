package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

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
    private final AtomicLong assetCounter;

    protected Crawler(C client, CrawlerConfig crawlerConfig) {
        this.client = checkNotNull(client);
        this.crawlerConfig = checkNotNull(crawlerConfig);
        assetCounter = new AtomicLong(0L);
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

    private boolean isUnderAssetCountLimit() {
        long limit = crawlerConfig.getAssetCountLimit();
        return assetCounter.get() < limit;
    }

    protected final void crawl(int queueCapacity) throws CrawlerException, X {
        Iterator<CrawlNode<?, X>> seeds = getSeedGenerator();
        BlockingQueue<CrawlNode<?, X>> queue = new ArrayBlockingQueue<>(queueCapacity);
        while (isUnderAssetCountLimit() && seeds.hasNext()) {
            CrawlNode<?, X> seed = seeds.next();
            queue.add(seed);
            while (isUnderAssetCountLimit() && !queue.isEmpty()) {
                CrawlNode<?, X> target = queue.remove();
                Iterable<CrawlNode<?, X>> branches = acquire(target);
                branches.forEach(queue::offer);
            }
            log.debug("proceeding to next seed because queue exhausted or asset limit reached");
        }
        log.debug("seeds exhausted or asset limit reached; terminating crawl");
    }

    protected Iterator<CrawlNode<?, X>> getSeedGenerator() {
        return ImmutableList.<CrawlNode<?, X>>of().iterator();
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

    protected VisitRecorder getVisitRecorder() {
        return crawlerConfig.getVisitRecorder();
    }

    protected <T> Iterable<CrawlNode<?, X>> acquire(CrawlNode<T, X> crawlNode) throws X {
        String category = crawlNode.getCategory();
        Throttler throttler = getThrottler();
        throttler.delay(category);
        try {
            T asset = crawlNode.call();
            String identifier = crawlNode.identify(asset);
            boolean newVisit = getVisitRecorder().recordVisit(identifier);
            if (asset != null && newVisit) {
                getAssetProcessor().process(asset, Iterables.concat(Collections.singleton(category), crawlNode.getLineage(asset)));
                assetCounter.incrementAndGet();
                Collection<CrawlNode<?, X>> branches = crawlNode.findNextTargets(asset);
                log.debug("acquired asset with identifier={}; has {} branches", identifier, branches.size());
                return branches;
            }
        } catch (RuntimeException e) {
            getErrorReactor().react(e);
        } catch (Exception exception) {
            X typedException;
            try {
                //noinspection unchecked
                typedException = (X) exception; // enforced by Action.call sgnature
            } catch (ClassCastException classCastException) {
                throw new AssertionError("expected different exception class from " + exception, classCastException);
            }
            maybeHandleRateLimitException(typedException);
            getErrorReactor().react(typedException);
        }
        return ImmutableList.of();
    }

    protected void maybeHandleRateLimitException(X exception) {
        // no op; subclasses override to, e.g., wait for rate limit window reset
    }
}
