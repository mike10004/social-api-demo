package com.github.mike10004.socialapidemo;

import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

/**
 * Interface for classes that serialize objects downloaded during crawls.
 */
public interface AssetSerializer {

    /**
     * Serializes an object of unknown type and returns a byte source
     * supplying a stream to the serialized data.
     * @param asset the object to serialize
     * @return the byte source, or null if the object could not be serialized
     */
    @Nullable
    ByteSource serialize(Object asset);

    /**
     * Convenience method that creates a byte source from a string.
     * @param data the string
     * @return the byte source
     */
    @Nullable
    default ByteSource toByteSource(String data) {
        return toByteSource(CharSource.wrap(data));
    }

    /**
     * Convenience method that converts a char source to a byte source with
     * {@code utf-8} encoding.
     * @param charSource the character source
     * @return the byte source
     */
    @Nullable
    default ByteSource toByteSource(CharSource charSource) {
        return charSource.asByteSource(StandardCharsets.UTF_8);
    }

    /**
     * Convenience method that serializes an edge set.
     * @param edges the edge set
     */
    default ByteSource serializeEdgeSet(EdgeSet<?> edges) {
        return toByteSource(new CharSource() {

            private transient final Gson gson = new GsonBuilder().setPrettyPrinting().create();
            private String json;

            @Override
            public synchronized Reader openStream() throws IOException {
                if (json == null) {
                    json = gson.toJson(edges);
                }
                return new StringReader(json);
            }
        });
    }
}
