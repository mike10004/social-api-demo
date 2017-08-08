package com.github.mike10004.socialapidemo;

import twitter4j.Twitter;

public class TwitterCrawler extends Crawler<Twitter> {

    public TwitterCrawler(Twitter client) {
        super(client);
    }

    @Override
    public void crawl(CrawlerConfig crawlerConfig) throws CrawlerException {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
