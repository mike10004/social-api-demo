package com.github.mike10004.socialapidemo;

import facebook4j.Facebook;

public class FacebookCrawler extends Crawler<Facebook> {

    public FacebookCrawler(Facebook client) {
        super(client);
    }

    @Override
    public void crawl(CrawlerConfig crawlerConfig) throws CrawlerException {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
