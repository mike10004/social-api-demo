package com.github.mike10004.socialapidemo;

import javax.annotation.Nullable;

public interface Throttler {

    void delay(@Nullable String category);

    static Throttler inactive() {
        return category -> { /* no op */};
    }
}
