package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableSet;
import facebook4j.Facebook;
import facebook4j.FacebookException;

public class FacebookCrawler extends Crawler<Facebook, FacebookException> {

    private static final ImmutableSet<Integer> RATE_LIMIT_ERROR_CODES = ImmutableSet.of(4, 17, 341);

    public FacebookCrawler(Facebook client, CrawlerConfig crawlerConfig) {
        super(client, crawlerConfig);
    }

    protected boolean isRateLimitException(FacebookException exception) {
        int errorCode = exception.getErrorCode();
        return RATE_LIMIT_ERROR_CODES.contains(errorCode);
    }

    @Override
    protected void maybeHandleRateLimitException(FacebookException exception) {
        if (isRateLimitException(exception)) {
            throw new CrawlerException("facebook rate limit breached");
        }
    }
}
