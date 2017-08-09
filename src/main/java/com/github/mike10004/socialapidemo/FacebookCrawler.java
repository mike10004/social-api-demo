package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableSet;
import facebook4j.Facebook;
import facebook4j.FacebookException;

public class FacebookCrawler extends Crawler<Facebook, FacebookException> {

    private static final ImmutableSet<Integer> RATE_LIMIT_ERROR_CODES = ImmutableSet.of(4, 17, 341);

    public FacebookCrawler(Facebook client, CrawlerConfig crawlerConfig) {
        super(client, crawlerConfig, buildThrottler(crawlerConfig), buildAssetProcessor(crawlerConfig));
    }

    @Override
    public void crawl() throws CrawlerException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    protected RateLimitReaction<FacebookException> getRateLimitReaction(FacebookException exception) {
        return RateLimitReaction.fail(exception);
    }

    @Override
    protected boolean isRateLimitException(FacebookException exception) {
        int errorCode = exception.getErrorCode();
        return RATE_LIMIT_ERROR_CODES.contains(errorCode);
    }

    protected static Throttler buildThrottler(CrawlerConfig crawlerConfig) {
        throw new UnsupportedOperationException();
    }

    protected static AssetProcessor buildAssetProcessor(CrawlerConfig crawlerConfig) {
        throw new UnsupportedOperationException();
    }
}
