package com.leekwars.api.logging;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Wrapper handler that logs requests before delegating to the actual handler
 */
public class LoggingHandler implements HttpHandler {
    private final HttpHandler delegate;

    public LoggingHandler(HttpHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        if (query != null && !query.isEmpty()) {
            System.out.println("[" + method + "] " + path + "?" + query);
        } else {
            System.out.println("[" + method + "] " + path);
        }

        delegate.handle(exchange);
    }
}