package com.leekwars.api.middleware;

import java.io.IOException;
import java.io.OutputStream;

import com.leekwars.api.mongo.config.MongoClientProvider;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class MongoHandler implements HttpHandler {
    private final MongoClientProvider mongo;
    private final HttpHandler delegate;

    public MongoHandler(MongoClientProvider mongo, HttpHandler delegate) {
        this.mongo = mongo;
        this.delegate = delegate;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (mongo == null || !mongo.isConnected()) {
            String msg = "Database unavailable";
            exchange.sendResponseHeaders(503, msg.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(msg.getBytes());
            }
            return;
        }

        delegate.handle(exchange);
    }
}
