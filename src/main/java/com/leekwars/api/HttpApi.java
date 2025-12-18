package com.leekwars.api;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.leekwars.api.logging.LoggingHandler;
import com.leekwars.generator.Generator;

import leekscript.compiler.LeekScript;
import leekscript.compiler.resolver.NativeFileSystem;

public class HttpApi {
    private static Generator generator;
    private static final int DEFAULT_PORT = 8080;
    private static int port = DEFAULT_PORT;
    private static boolean runTests = false;

    public static void main(String[] args) throws IOException {
        System.out.println("Starting LeekScript Code Analysis Server...");
        // Check for test flag
        for (String arg : args) {
            if ("--start_tests".equals(arg)) {
                runTests = true;
                break;
            }


            if (arg.startsWith("--port=")) {
                try {
                    int portValue = Integer.parseInt(arg.substring(7));
                    if (portValue > 0 && portValue <= 65535) {
                        new HttpApi().port = portValue;
                    } else {
                        System.err.println("Invalid port number. Using default port " + DEFAULT_PORT);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port format. Using default port " + DEFAULT_PORT);
                }
            }
        }

        LeekScript.setFileSystem(new NativeFileSystem());

        // Initialize services
        generator = new Generator();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new LoggingHandler(new RootHandler()));

        server.setExecutor(null); // creates a default executor
        server.start();

        System.out.println("LeekScript Code Analysis Server started on port " + port);
        System.out.println("Ready to receive requests...");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    // Root handler
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "LeekScript Code Analysis Server is running";
            sendResponse(exchange, 200, response);
        }
    }
}
