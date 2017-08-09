package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableSet;
import facebook4j.Facebook;
import facebook4j.FacebookException;

public class FacebookCrawler extends Crawler<Facebook, FacebookException> {

    private static final ImmutableSet<Integer> RATE_LIMIT_ERROR_CODES = ImmutableSet.of(4, 17, 341);

    public FacebookCrawler(Facebook client, CrawlerConfig crawlerConfig) {
        super(client, crawlerConfig);
    }

    @Override
    public void crawl() throws CrawlerException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    protected boolean isRateLimitException(FacebookException exception) {
        int errorCode = exception.getErrorCode();
        return RATE_LIMIT_ERROR_CODES.contains(errorCode);
    }

}
