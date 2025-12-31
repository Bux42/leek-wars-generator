package com.leekwars.api;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.logging.LoggingHandler;
import com.leekwars.generator.Generator;
import com.leekwars.pool.leek.Leek;

import leekscript.compiler.AIFile;
import leekscript.compiler.IACompiler.AnalyzeResult;
import leekscript.compiler.LeekScript;
import leekscript.compiler.resolver.NativeFileSystem;

public class HttpApi {
    private static Generator generator;
    private static final int DEFAULT_PORT = 8080;
    private static int port = DEFAULT_PORT;

    public static void main(String[] args) throws IOException {
        System.out.println("Starting LeekScript Code Analysis Server...");
        // Check for test flag
        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                try {
                    int portValue = Integer.parseInt(arg.substring(7));
                    if (portValue > 0 && portValue <= 65535) {
                        new HttpApi();
                        HttpApi.port = portValue;
                    } else {
                        System.err.println("Invalid port number. Using default port " + DEFAULT_PORT);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port format. Using default port " + DEFAULT_PORT);
                }
            }
        }

        LeekScript.setFileSystem(new NativeFileSystem());
        ensureDirectoryExists("user-code");

        // Initialize services
        generator = new Generator();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Ping endpoint
        server.createContext("/", new LoggingHandler(new RootHandler()));

        // Analyze AI code and return analysis endpoint
        server.createContext("/api/ai/analyze-file", new LoggingHandler(new AiAnalyzeFileHandler()));

        // CodeDefinition enpoints
        server.createContext("/api/get-definitions", new LoggingHandler(new CodeDefinitionHandler()));

        // Pools
        server.createContext("/api/get-pools", new LoggingHandler(new PoolsHandler()));

        // Leeks
        server.createContext("/api/get-leeks", new LoggingHandler(new LeeksHandler()));

        server.setExecutor(null); // creates a default executor
        server.start();

        System.out.println("LeekScript Code Analysis Server started on port " + port);
        System.out.println("Ready to receive requests...");
    }

    // check if directory exists, if not create it
    private static void ensureDirectoryExists(String path) {
        java.io.File dir = new java.io.File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // Get request body as JSON object
    private static JSONObject readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        return JSON.parseObject(body);
    }

    // Send plain text response
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    // Send JSON response
    private static void sendJsonResponse(HttpExchange exchange, int statusCode, Object jsonObject) throws IOException {
        String jsonString = JSON.toJSONString(jsonObject);
        byte[] responseBytes = jsonString.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
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

    // AI analyze file handler - analyzes a file from disk using NativeFileSystem
    static class AiAnalyzeFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }

            try {
                JSONObject json = readRequestBody(exchange);
                String filePath = json.getString("file_path");

                if (filePath == null || filePath.isEmpty()) {
                    sendResponse(exchange, 400, "Missing file_path parameter");
                    return;
                }

                System.out.println(
                        "Analyzing file from disk: " + filePath + " hascode: " + (filePath.hashCode() & 0xfffffff));

                // Use NativeFileSystem to resolve the file from disk
                // This mimics the --analyze functionality from Main.java
                NativeFileSystem nativeFs = new NativeFileSystem();
                LeekScript.setFileSystem(nativeFs);

                try {
                    // Resolve the file through the filesystem
                    AIFile aiFile = nativeFs.getRoot().resolve(filePath);

                    // Analyze the file
                    AnalyzeResult analyzeResult = generator.analyzeAI(aiFile, 0);
                    JSONArray errors = formatErrors(analyzeResult);

                    // Build response - using a synthetic AI ID based on file path hash
                    int syntheticId = filePath.hashCode() & 0x7FFFFFFF; // Positive integer
                    JSONObject response = new JSONObject();
                    JSONObject result = new JSONObject();
                    result.put(String.valueOf(syntheticId), errors);
                    response.put("result", result);
                    response.put("modified", System.currentTimeMillis());
                    response.put("file_path", filePath);
                    response.put("success", analyzeResult.success);

                    sendJsonResponse(exchange, 200, response);

                } catch (FileNotFoundException e) {
                    System.err.println("File not found: " + filePath);
                    sendResponse(exchange, 404, "File not found: " + filePath);
                }

            } catch (Exception e) {
                System.err.println("Error in AiAnalyzeFileHandler: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }

    /**
     * Format analyze result errors for API response
     * The errors are already in the correct format from IACompiler
     */
    public static JSONArray formatErrors(AnalyzeResult result) {
        if (result == null || result.informations == null) {
            return new JSONArray();
        }
        return result.informations;
    }

    // CodeDefinition endpoints
    static class CodeDefinitionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }

            try {
                JSONObject json = readRequestBody(exchange);

                int cursorLine = json.getIntValue("cursor_line");
                int cursorColumn = json.getIntValue("cursor_column");
                String file_path = json.getString("file_path");
                String file_code = json.getString("file_code");

                System.out.println("CodeDefinition request for file: " + file_path + " at line: " + cursorLine
                        + ", column: " + cursorColumn);

                // System.out.println("File code:\n" + file_code);

                // mock file_code with JSON.stringify + replace double backslashes with single
                // backslashes

                NativeFileSystem nativeFs = new NativeFileSystem();
                LeekScript.setFileSystem(nativeFs);

                try {
                    // Resolve the file through the filesystem
                    // AIFile aiFile = nativeFs.getRoot().resolve(file_path);

                    // debug mode
                    boolean debug = false;

                    var ai = LeekScript.getFileSystem().getRoot().resolve(file_path);
                    ai.setCode(file_code);
                    leekscript.compiler.vscode.DefinitionsResult result = generator.getDefinitions(ai,
                            cursorLine,
                            cursorColumn, debug);

                    if (debug) {
                        String jsonResponse = JSON.toJSONString(result, true);
                        System.out.println("DefinitionsResult JSON:\n" + jsonResponse);
                    }

                    sendJsonResponse(exchange, 200, result);
                } catch (FileNotFoundException e) {
                    System.err.println("File not found for CodeDefinition: " + file_path);
                    sendResponse(exchange, 404, "File not found: " + file_path);
                    return;
                }

            } catch (Exception e) {
                System.err.println("Error in CodeDefinitionHandler: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }

    // Pools endpoints
    static class PoolsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }

            try {
                JSONObject json = readRequestBody(exchange);

                // For demonstration, we return a static list of pools
                JSONArray pools = new JSONArray();
                pools.add("Pool A");
                pools.add("Pool B");
                pools.add("Pool C");

                JSONObject response = new JSONObject();
                response.put("pools", pools);
                response.put("success", true);

                sendJsonResponse(exchange, 200, response);

            } catch (Exception e) {
                System.err.println("Error in PoolsHandler: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }

    // Leeks endpoints
    static class LeeksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            try {
                JSONObject json = readRequestBody(exchange);

                // For demonstration, we return a static list of leeks
                JSONArray leeks = new JSONArray();
                Leek e = new Leek();
                e.name = "Leek 1";
                leeks.add(e);
                leeks.add(new Leek());
                leeks.add(new Leek());
                leeks.add(new Leek());
                
                JSONObject response = new JSONObject();
                response.put("leeks", leeks);
                response.put("success", true);

                sendJsonResponse(exchange, 200, response);

            } catch (Exception e) {
                System.err.println("Error in LeeksHandler: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }   
}