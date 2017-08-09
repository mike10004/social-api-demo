package com.github.mike10004.socialapidemo;

import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

public interface AssetSerializer {

    @Nullable
    ByteSource serialize(Object asset);

    @Nullable
    default ByteSource serialize(String data) {
        return serialize(CharSource.wrap(data));
    }

    @Nullable
    default ByteSource serialize(CharSource charSource) {
        return charSource.asByteSource(StandardCharsets.UTF_8);
    }

}
