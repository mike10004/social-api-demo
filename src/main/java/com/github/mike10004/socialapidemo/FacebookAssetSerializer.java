package com.github.mike10004.socialapidemo;

import com.google.common.io.ByteSource;
import facebook4j.FacebookResponse;
import facebook4j.internal.org.json.JSONException;
import facebook4j.internal.org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacebookAssetSerializer implements AssetSerializer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public ByteSource serialize(Object asset) {
        try {
            if (asset instanceof FacebookResponse) {
                JSONObject jsonObject = ((FacebookResponse) asset).getJSON();
                String json = jsonObject.toString(2);
                return toByteSource(json);
            } else if (asset instanceof EdgeSet) {
                return serializeEdgeSet((EdgeSet<?>) asset);
            } else {
                log.debug("not serializing object of {}", (asset == null ? "<null>" : asset.getClass()));
            }
        } catch (RuntimeException | JSONException e) {
            log.info("failed to serialize " + asset, e);
        }
        return null;
    }
}
