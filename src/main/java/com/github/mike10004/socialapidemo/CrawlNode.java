package com.github.mike10004.socialapidemo;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class CrawlNode<T, E extends Exception> implements CheckedCallable<T, E> {

    @Nullable
    private final String category;

    @SuppressWarnings("unused")
    protected CrawlNode() {
        this(null);
    }

    protected CrawlNode(@Nullable String category) {
        this.category = category;
    }

    @Nullable
    public String getCategory() {
        return category;
    }
    public Iterable<String> getLineage(T asset) {
        return ImmutableList.of();
    }

    public Collection<CrawlNode<?, E>> findNextTargets(T asset) throws E {
        return ImmutableList.of();
    }

    private static final Joiner identityJoiner = Joiner.on('/');

    public String identify(T asset) {
        ImmutableList<String> lineage = ImmutableList.copyOf(getLineage(asset));
        if (lineage.isEmpty()) {
            return String.valueOf(asset.hashCode());
        } else {
            String category = getCategory();
            Object[] parts = Stream.concat(Stream.of(category), lineage.stream()).filter(Objects::nonNull).toArray();
            return identityJoiner.join(parts);
        }
    }
}
