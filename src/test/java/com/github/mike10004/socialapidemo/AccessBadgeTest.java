package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

public class AccessBadgeTest {

    @Test
    public void getExtra() {
        assertNull(new AccessBadge(null, null, null).getExtra("hello"));
        assertEquals("bar", new AccessBadge(null, null, null, ImmutableMap.of("foo", "bar")).getExtra("foo"));
    }

    @Test
    public void serializeExpiry() throws Exception {
        Instant original = Instant.now();
        AccessBadge b = new AccessBadge("abc", "def", original);
        JsonObject btree = new Gson().toJsonTree(b).getAsJsonObject();
        JsonElement instantEl = btree.get("expiry");
        assertTrue("is string", instantEl.getAsJsonPrimitive().isString());
        System.out.format("instant: %s%n", instantEl.getAsString());
        AccessBadge a = new Gson().fromJson(btree, AccessBadge.class);
        Instant deserialized = a.expiry;
        assertEquals("instant", original, deserialized);
    }
}