package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public interface ErrorReactor {

    <X extends Exception> void react(X exception) throws X, CrawlerException;

    static ErrorReactor rethrower() {
        return new ErrorReactor() {
            @Override
            public <X extends Exception> void react(X exception) throws X {
                throw exception;
            }
        };
    }

    static LimitedErrorReactor limiter(int maxErrors) {
        return new LimitedErrorReactor(maxErrors);
    }

    class LimitedErrorReactor implements ErrorReactor {

        private static final int MAX_RETAINED_EXCEPTIONS = 32;

        private final long maxErrors;
        private final AtomicLong errorCounter;
        private final Map<Long, Exception> exceptions;
        public LimitedErrorReactor(long maxErrors) {
            this.maxErrors = maxErrors;
            errorCounter = new AtomicLong(0L);
            int maxRetainedExceptions = Math.min(MAX_RETAINED_EXCEPTIONS, Ints.saturatedCast(maxErrors));
            exceptions = new LinkedHashMap<Long, Exception>(maxRetainedExceptions) {
                @Override
                protected boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > maxRetainedExceptions;
                }
            };
        }

        @Override
        public <X extends Exception> void react(X exception) throws X, CrawlerException {
            long count = errorCounter.incrementAndGet();
            exceptions.put(count, exception);
            if (count > maxErrors) {
                throw new TooManyErrorsException();
            }
        }

        public ImmutableList<Exception> getRetainedExceptions() {
            return ImmutableList.copyOf(exceptions.values());
        }

        public class TooManyErrorsException extends CrawlerException {

            public TooManyErrorsException() {
                super("reached limit of " + maxErrors + " error(s)");
            }
        }
    }
}
