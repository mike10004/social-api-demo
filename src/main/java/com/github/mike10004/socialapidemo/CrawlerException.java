package com.github.mike10004.socialapidemo;

@SuppressWarnings("unused")
public class CrawlerException extends ExerciseException {

    public CrawlerException(String message) {
        super(message);
    }

    public CrawlerException(String message, Throwable cause) {
        super(message, cause);
    }

    public CrawlerException(Throwable cause) {
        super(cause);
    }
}
