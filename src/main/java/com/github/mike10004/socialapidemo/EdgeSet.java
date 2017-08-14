package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 * @param <E> enumeration of relationship types
 */
public class EdgeSet<E extends Enum> {

    public final String sourceId;

    public final ImmutableMultimap<E, String> targetIds;

    public EdgeSet(String sourceId, Multimap<E, String> targetIds) {
        this.sourceId = checkNotNull(sourceId);
        this.targetIds = ImmutableMultimap.copyOf(targetIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EdgeSet<?> edgeSet = (EdgeSet<?>) o;

        if (!sourceId.equals(edgeSet.sourceId)) return false;
        return targetIds.equals(edgeSet.targetIds);
    }

    @Override
    public int hashCode() {
        int result = sourceId.hashCode();
        result = 31 * result + targetIds.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "EdgeSet{" +
                "sourceId='" + sourceId + '\'' +
                ", targetIds=" + targetIds.values().size() +
                '}';
    }
}
