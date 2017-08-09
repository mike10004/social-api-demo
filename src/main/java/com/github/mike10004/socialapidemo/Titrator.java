package com.github.mike10004.socialapidemo;

import java.time.Duration;

public interface Titrator {

    void consume();

    boolean tryConsume();

    static Titrator create(int numDosesPerPeriod, Duration period) {
        return new CountingTitrator(numDosesPerPeriod, period);
    }

    static double durationToFractionalSeconds(Duration period) {
        return period.getSeconds() + period.getNano() / 1000000000d;
    }
}
