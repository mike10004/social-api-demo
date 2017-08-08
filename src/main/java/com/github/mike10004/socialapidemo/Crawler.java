package com.github.mike10004.socialapidemo;

/**
 *
 * @param <C> client type
 */
public abstract class Crawler<C> {

    protected final C client;

    protected Crawler(C client) {
        this.client = client;
    }

    public abstract void crawl(CrawlerConfig crawlerConfig) throws CrawlerException;
}
