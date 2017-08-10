package com.github.mike10004.socialapidemo;

import javax.annotation.Nullable;

public interface Throttler {

    void delay(@Nullable String category);

    static Throttler inactive() {
        return new Throttler() {
            @Override
            public void delay(@Nullable String category) {
                // no op
            }

            @Override
            public String toString() {
                return "InactiveThrottler{}";
            }
        };
    }
}
