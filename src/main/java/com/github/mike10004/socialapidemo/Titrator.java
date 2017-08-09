package com.github.mike10004.socialapidemo;

import java.time.Duration;

public interface Titrator {

    void consume();

    boolean tryConsume();

    static Titrator create(int numDosesPerPeriod, Duration period) {
        return new QueuedTitrator(numDosesPerPeriod, period);
    }

}
