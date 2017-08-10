package com.github.mike10004.socialapidemo;

import com.google.common.util.concurrent.Uninterruptibles;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public interface Sleeper {

    void sleep(Duration duration);

    static Sleeper system() {
        return duration -> Uninterruptibles.sleepUninterruptibly(duration.toMillis(), TimeUnit.MILLISECONDS);
    }
}
