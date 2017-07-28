package com.github.mike10004.socialapidemo;

import java.io.IOException;
import java.net.URL;

public interface HttpGetter {

    class SimpleResponse {
        public final URL url;
        public final int status;
        public final String text;

        public SimpleResponse(URL url, int status, String text) {
            this.url = url;
            this.status = status;
            this.text = text;
        }

        @Override
        public String toString() {
            return "SimpleResponse{" +
                    "url=" + url +
                    ", status=" + status +
                    ", text='" + text + '\'' +
                    '}';
        }
    }

    SimpleResponse executeGet(URL url) throws IOException;
}
