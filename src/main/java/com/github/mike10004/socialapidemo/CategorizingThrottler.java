package com.github.mike10004.socialapidemo;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import javax.annotation.Nullable;
import java.time.Duration;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class CategorizingThrottler<T> implements Throttler {

    private final LoadingCache<String, T> permitAcquirerMap;
    private final Sleeper marginSleeper;
    private final Duration marginDuration;

    /**
     * Constructs an instance.
     * @param marginDuration duration to sleep before each delay, as an extra margin to stay under the rate limits
     * @param marginSleeper sleeper to use when sleeping for the margin duration
     */
    protected CategorizingThrottler(Duration marginDuration, Sleeper marginSleeper) {
        this.permitAcquirerMap = CacheBuilder.newBuilder()
                .build(new CacheLoader<String, T>() {
                    @Override
                    public T load(String key) throws Exception {
                        return createPermitAcquirer(key);
                    }
                });
        this.marginDuration = checkNotNull(marginDuration);
        this.marginSleeper = checkNotNull(marginSleeper);
    }

    protected abstract void acquire(T permitAcquirer);

    @Override
    public void delay(@Nullable String category) {
        category = Strings.nullToEmpty(category);
        T permitAcquirer = permitAcquirerMap.getUnchecked(category);
        maybeSleepForMargin();
        acquire(permitAcquirer);
    }

    private void maybeSleepForMargin() {
        if (!marginDuration.isZero()) {
            marginSleeper.sleep(marginDuration);
        }
    }

    protected abstract T createPermitAcquirer(String category);

}
