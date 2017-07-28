package com.github.mike10004.socialapidemo;

import com.google.common.net.HostAndPort;
import fi.iki.elonen.NanoHTTPD;
import org.junit.rules.ExternalResource;

import javax.annotation.Nullable;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.google.common.base.Preconditions.checkState;

public class NanoHttpdRule extends ExternalResource {

    private NanoHTTPD server;

    private final List<RequestHandler> requestHandlers = new CopyOnWriteArrayList<>();
    private RequestHandler defaultRequestHandler = RequestHandler.getDefault();
    public interface RequestHandler {
        @Nullable
        NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session);

        NanoHTTPD.Response NOT_FOUND_RESPONSE = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "404 Not Found");

        static RequestHandler getDefault() {
            return (session) -> NOT_FOUND_RESPONSE;
        }

    }

    public static NanoHTTPD.Response respond(int status, String contentType, String contentText) {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.lookup(status), contentType, contentText);
    }

    @Override
    protected void before() throws Throwable {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        checkState( port > 0 && port < 65536, "port %s", port);
        server = new NanoHTTPD(port) {
            @Override
            public Response serve(IHTTPSession session) {
                for (RequestHandler handler : requestHandlers) {
                    Response response = handler.serve(session);
                    if (response != null) {
                        return response;
                    }
                }
                return defaultRequestHandler.serve(session);
            }
        };
        server.start();
    }

    @Override
    protected void after() {
        if (server != null && server.wasStarted()) {
            server.stop();
        }
    }

    @SuppressWarnings("unused")
    public NanoHTTPD getServer() {
        return server;
    }

    public HostAndPort getSocketAddress() {
        checkState(server != null, "server not instantiated yet");
        return HostAndPort.fromParts("localhost", server.getListeningPort());
    }

    @SuppressWarnings("UnusedReturnValue")
    public NanoHttpdRule handle(RequestHandler requestHandler) {
        requestHandlers.add(requestHandler);
        return this;
    }

    public URIBuilder buildUri() {
        return new URIBuilder("http://" + getSocketAddress() + "/");
    }
}
