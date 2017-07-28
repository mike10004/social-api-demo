package com.github.mike10004.socialapidemo;

import com.google.common.annotations.VisibleForTesting;

import java.io.PrintStream;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class Demonstrator<C> {

    protected final C client;
    protected final PrintStream output;

    protected Demonstrator(C client) {
        this(client, System.out);
    }

    @VisibleForTesting
    protected Demonstrator(C client, PrintStream output) {
        this.client = checkNotNull(client);
        this.output = checkNotNull(output);
    }

    public abstract void demonstrate() throws ActivityException;

}
