package com.github.mike10004.socialapidemo;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

public abstract class HttpGetterTestBase {

    @Rule
    public NanoHttpdRule httpdRule = new NanoHttpdRule();

    @Test
    public void executeGet() throws Exception {
        String simulatedHttpbinGetResponseText = "{\n" +
                "  \"args\": {}, \n" +
                "  \"headers\": {\n" +
                "    \"Accept\": \"text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2\", \n" +
                "    \"Accept-Encoding\": \"gzip\", \n" +
                "    \"Connection\": \"close\", \n" +
                "    \"Host\": \"httpbin.org\", \n" +
                "    \"User-Agent\": \"facebook4j http://facebook4j.org/ /2.4.10\"\n" +
                "  }, \n" +
                "  \"origin\": \"38.122.8.30\", \n" +
                "  \"url\": \"https://httpbin.org/get\"\n" +
                "}";
        httpdRule.handle(session -> {
            if ("/get".equals(session.getUri())) {
                return NanoHttpdRule.respond(200, "application/json", simulatedHttpbinGetResponseText);
            }
            return null;
        });
        URL url = httpdRule.buildUri().setPath("/get").build().toURL();
        HttpGetter getter = buildGetter();
        HttpGetter.SimpleResponse response = getter.executeGet(url);
        System.out.println(response.text);
        assertEquals("response status", 200, response.status);
    }

    protected abstract HttpGetter buildGetter() throws IOException;
}
