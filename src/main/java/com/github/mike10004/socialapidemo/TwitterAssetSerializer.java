package com.github.mike10004.socialapidemo;

import com.google.common.io.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.JSONException;
import twitter4j.JSONObject;
import twitter4j.TwitterResponse;

public class TwitterAssetSerializer implements AssetSerializer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public ByteSource serialize(Object asset) {
        try {
            if (asset instanceof TwitterResponse) {
                JSONObject jsonObject = ((TwitterResponse) asset).getJson();
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
