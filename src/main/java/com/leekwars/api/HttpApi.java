package com.leekwars.api;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.files.FileManager;
import com.leekwars.api.files.FileManager.FileInfo;
import com.leekwars.api.logging.LoggingHandler;
import com.leekwars.api.mongo.MongoDbManager;
import com.leekwars.api.mongo.scenarios.PoolOneVersusOne;
import com.leekwars.generator.Generator;
import com.leekwars.pool.leek.Leek;
import org.bson.Document;

import leekscript.compiler.AIFile;
import leekscript.compiler.IACompiler.AnalyzeResult;
import leekscript.compiler.LeekScript;
import leekscript.compiler.resolver.NativeFileSystem;

public class HttpApi {
    private static Generator generator;
    private static final int DEFAULT_PORT = 8080;
    private static int port = DEFAULT_PORT;
    private static FileManager fileManager = new FileManager();
    private static MongoDbManager mongoDbManager;

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
        
        // Initialize MongoDB connection
        mongoDbManager = new MongoDbManager("mongodb://localhost:27017");
        if (mongoDbManager.connect("leekwars")) {
            System.out.println("Connected to MongoDB database: leekwars");
        } else {
            System.err.println("Failed to connect to MongoDB. Leek operations will not be available.");
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Ping endpoint
        server.createContext("/", new LoggingHandler(new RootHandler()));

        // Analyze AI code and return analysis endpoint
        server.createContext("/api/ai/analyze-file", new LoggingHandler(new AiAnalyzeFileHandler()));

        // CodeDefinition enpoints
        server.createContext("/api/get-definitions", new LoggingHandler(new CodeDefinitionHandler()));

        // Pools
        server.createContext("/api/get-pools", new LoggingHandler(new PoolsHandler()));
        server.createContext("/api/pool1v1/add", new LoggingHandler(new AddPool1v1Handler()));
        server.createContext("/api/pool1v1/list", new LoggingHandler(new ListPool1v1Handler()));
        server.createContext("/api/pool1v1/update", new LoggingHandler(new UpdatePool1v1Handler()));
        server.createContext("/api/pool1v1/delete", new LoggingHandler(new DeletePool1v1Handler()));
        server.createContext("/api/pool1v1/clear-stats", new LoggingHandler(new ClearPool1v1StatsHandler()));
        server.createContext("/api/pool1v1/set-enabled", new LoggingHandler(new SetPool1v1EnabledHandler()));

        // Leeks
        server.createContext("/api/get-leeks", new LoggingHandler(new LeeksHandler()));
        server.createContext("/api/add-leek", new LoggingHandler(new AddLeekHandler()));

        // File browsing endpoints
        server.createContext("/api/file/list", new LoggingHandler(new FileListHandler()));
        server.createContext("/api/file/reset", new LoggingHandler(new FileResetHandler()));

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

    /**
     * Helper method to populate leek objects in a pool from their IDs
     * @param poolJson The pool JSON object containing leek_ids
     * @return Pool JSON with leeks array populated with full leek objects
     */
    private static JSONObject populateLeeksInPool(JSONObject poolJson) {
        if (!poolJson.containsKey("leek_ids")) {
            return poolJson;
        }

        JSONArray leekIds = poolJson.getJSONArray("leek_ids");
        JSONArray leeks = new JSONArray();

        for (int i = 0; i < leekIds.size(); i++) {
            String leekId = leekIds.getString(i);
            Document leekDoc = mongoDbManager.getLeekById(leekId);
            
            if (leekDoc != null) {
                String leekJson = leekDoc.toJson();
                JSONObject leekObject = JSON.parseObject(leekJson);
                leekObject.remove("_id");
                leeks.add(leekObject);
            } else {
                // If leek not found, add a placeholder with just the ID
                JSONObject placeholder = new JSONObject();
                placeholder.put("id", leekId);
                placeholder.put("name", "Unknown Leek");
                placeholder.put("error", "Leek not found");
                leeks.add(placeholder);
            }
        }

        poolJson.put("leeks", leeks);
        return poolJson;
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
                // Check if MongoDB is connected
                if (mongoDbManager == null || !mongoDbManager.isConnected()) {
                    sendResponse(exchange, 503, "Database not available");
                    return;
                }

                // Get all leeks from the database
                var leeksCollection = mongoDbManager.getCollection("leeks");
                JSONArray leeks = new JSONArray();
                
                for (Document doc : leeksCollection.find()) {
                    // Convert MongoDB document to JSON
                    String docJson = doc.toJson();
                    JSONObject leekJson = JSON.parseObject(docJson);
                    
                    // Remove MongoDB's _id field and use our custom id field
                    leekJson.remove("_id");
                    
                    leeks.add(leekJson);
                }
                
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

    // Add Leek endpoint
    static class AddLeekHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            try {
                JSONObject json = readRequestBody(exchange);

                // Check if MongoDB is connected
                if (mongoDbManager == null || !mongoDbManager.isConnected()) {
                    sendResponse(exchange, 503, "Database not available");
                    return;
                }

                // Deserialize JSON into Leek object using fromJson method
                Leek leek = Leek.fromJson(json);

                // Validate required fields
                if (leek.name == null || leek.name.isEmpty()) {
                    sendResponse(exchange, 400, "Missing required field: name");
                    return;
                }

                // Set elo to default value (100)
                leek.elo = 100;

                // Convert the entire Leek object to JSON and then to MongoDB Document
                String leekJson = JSON.toJSONString(leek);
                Document leekData = Document.parse(leekJson);

                // Add leek to database
                String leekId = mongoDbManager.addLeek(leekData);

                if (leekId != null) {
                    // Set the ID on the leek object
                    leek.id = leekId;

                    // Return the newly created leek as JSON
                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("leek", JSON.parseObject(JSON.toJSONString(leek)));

                    System.out.println("Successfully added leek: " + leek.name + " with ID: " + leekId);
                    sendJsonResponse(exchange, 201, response);
                } else {
                    sendResponse(exchange, 500, "Failed to add leek to database");
                }

            } catch (Exception e) {
                System.err.println("Error in AddLeekHandler: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }

    // ----------- FILES ENDPOINTS ----------------

    // File list handler
    static class FileListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            try {
                JSONObject json = readRequestBody(exchange);
                String directoryPath = json.getString("directory_path");


                if (directoryPath == null || directoryPath.isEmpty()) {
                    sendResponse(exchange, 400, "Missing directory_path parameter");
                    return;
                }

                System.out.println("Listing files in directory: " + directoryPath);

                List<FileInfo> files = fileManager.listAll(directoryPath);

                JSONArray filesArray = new JSONArray();

                for (FileInfo file : files) {
                    JSONObject fileJson = new JSONObject();
                    fileJson.put("name", file.getName());
                    fileJson.put("path", file.getPath());
                    fileJson.put("directory", file.isDirectory());
                    filesArray.add(fileJson);
                }

                JSONObject response = new JSONObject();
                response.put("files", filesArray);
                response.put("success", true);

                sendJsonResponse(exchange, 200, response);

            } catch (Exception e) {
                System.err.println("Error in FileListHandler: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }

    // Reset to root directory handler
    static class FileResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            try {
                fileManager.resetToRoot();
                List<FileInfo> files = fileManager.listAll();

                JSONArray filesArray = new JSONArray();

                for (FileInfo file : files) {
                    JSONObject fileJson = new JSONObject();
                    fileJson.put("name", file.getName());
                    fileJson.put("path", file.getPath());
                    fileJson.put("directory", file.isDirectory());
                    filesArray.add(fileJson);
                }

                JSONObject response = new JSONObject();
                response.put("files", filesArray);
                response.put("success", true);

                sendJsonResponse(exchange, 200, response);
            } catch (Exception e) {
                System.err.println("Error in FileResetHandler: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }

    // ========== POOL 1V1 ENDPOINTS ==========

    // Add Pool 1v1 endpoint
    static class AddPool1v1Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            try {
                JSONObject json = readRequestBody(exchange);

                // Check if MongoDB is connected
                if (mongoDbManager == null || !mongoDbManager.isConnected()) {
                    sendResponse(exchange, 503, "Database not available");
                    return;
                }

                // Validate required fields
                JSONArray leekIds = json.getJSONArray("leek_ids");
                String name = json.getString("name");

                if (leekIds == null || name == null || name.isEmpty()) {
                    sendResponse(exchange, 400, "Missing required fields: leek_ids (non-empty array), name");
                    return;
                }

                // Deserialize JSON into PoolOneVersusOne object
                PoolOneVersusOne pool = PoolOneVersusOne.fromJson(json);

                // Convert to MongoDB Document
                String poolJson = JSON.toJSONString(pool);
                Document poolData = Document.parse(poolJson);

                // Add pool to database
                String poolId = mongoDbManager.addPool1v1(poolData);

                if (poolId != null) {
                    pool.id = poolId;

                    // Populate leek objects
                    JSONObject poolResponse = populateLeeksInPool(JSON.parseObject(JSON.toJSONString(pool)));

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("pool", poolResponse);

                    System.out.println("Successfully added 1v1 pool: " + pool.name + " with ID: " + poolId);
                    sendJsonResponse(exchange, 201, response);
                } else {
                    sendResponse(exchange, 500, "Failed to add 1v1 pool to database");
                }

            } catch (Exception e) {
                System.err.println("Error in AddPool1v1Handler: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }

    // List Pool 1v1 endpoint
    static class ListPool1v1Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            try {
                // Check if MongoDB is connected
                if (mongoDbManager == null || !mongoDbManager.isConnected()) {
                    sendResponse(exchange, 503, "Database not available");
                    return;
                }

                // Get all pools from the database
                var poolsCollection = mongoDbManager.getPool1v1Collection();
                JSONArray pools = new JSONArray();
                
                for (Document doc : poolsCollection.find()) {
                    String docJson = doc.toJson();
                    JSONObject poolJson = JSON.parseObject(docJson);
                    poolJson.remove("_id");
                    
                    // Populate leek objects
                    poolJson = populateLeeksInPool(poolJson);
                    
                    pools.add(poolJson);
                }
                
                JSONObject response = new JSONObject();
                response.put("pools", pools);
                response.put("success", true);

                sendJsonResponse(exchange, 200, response);

            } catch (Exception e) {
                System.err.println("Error in ListPool1v1Handler: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }

    // Update Pool 1v1 endpoint
    static class UpdatePool1v1Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            try {
                JSONObject json = readRequestBody(exchange);

                // Check if MongoDB is connected
                if (mongoDbManager == null || !mongoDbManager.isConnected()) {
                    sendResponse(exchange, 503, "Database not available");
                    return;
                }

                String poolId = json.getString("id");
                if (poolId == null || poolId.isEmpty()) {
                    sendResponse(exchange, 400, "Missing required field: id");
                    return;
                }

                // Remove id from updates
                json.remove("id");

                // Convert to Document
                Document updates = Document.parse(json.toJSONString());

                // Update pool in database
                boolean success = mongoDbManager.updatePool1v1(poolId, updates);

                if (success) {
                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Pool updated successfully");
                    sendJsonResponse(exchange, 200, response);
                } else {
                    sendResponse(exchange, 404, "Pool not found");
                }

            } catch (Exception e) {
                System.err.println("Error in UpdatePool1v1Handler: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }

    // Delete Pool 1v1 endpoint
    static class DeletePool1v1Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            try {
                JSONObject json = readRequestBody(exchange);

                // Check if MongoDB is connected
                if (mongoDbManager == null || !mongoDbManager.isConnected()) {
                    sendResponse(exchange, 503, "Database not available");
                    return;
                }

                String poolId = json.getString("id");
                if (poolId == null || poolId.isEmpty()) {
                    sendResponse(exchange, 400, "Missing required field: id");
                    return;
                }

                // Delete pool from database
                boolean success = mongoDbManager.deletePool1v1(poolId);

                if (success) {
                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Pool deleted successfully");
                    sendJsonResponse(exchange, 200, response);
                } else {
                    sendResponse(exchange, 404, "Pool not found");
                }

            } catch (Exception e) {
                System.err.println("Error in DeletePool1v1Handler: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }

    // Clear Pool 1v1 Stats endpoint
    static class ClearPool1v1StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            try {
                JSONObject json = readRequestBody(exchange);

                // Check if MongoDB is connected
                if (mongoDbManager == null || !mongoDbManager.isConnected()) {
                    sendResponse(exchange, 503, "Database not available");
                    return;
                }

                String poolId = json.getString("id");
                if (poolId == null || poolId.isEmpty()) {
                    sendResponse(exchange, 400, "Missing required field: id");
                    return;
                }

                // Clear stats for pool
                boolean success = mongoDbManager.clearPool1v1Stats(poolId);

                if (success) {
                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Pool stats cleared successfully");
                    sendJsonResponse(exchange, 200, response);
                } else {
                    sendResponse(exchange, 404, "Pool not found");
                }

            } catch (Exception e) {
                System.err.println("Error in ClearPool1v1StatsHandler: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }

    // Set Pool 1v1 Enabled Status endpoint
    static class SetPool1v1EnabledHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            try {
                JSONObject json = readRequestBody(exchange);

                // Check if MongoDB is connected
                if (mongoDbManager == null || !mongoDbManager.isConnected()) {
                    sendResponse(exchange, 503, "Database not available");
                    return;
                }

                String poolId = json.getString("id");
                if (poolId == null || poolId.isEmpty()) {
                    sendResponse(exchange, 400, "Missing required field: id");
                    return;
                }

                if (!json.containsKey("enabled")) {
                    sendResponse(exchange, 400, "Missing required field: enabled");
                    return;
                }

                boolean enabled = json.getBooleanValue("enabled");

                // Set enabled status for pool
                boolean success = mongoDbManager.setPool1v1Enabled(poolId, enabled);

                if (success) {
                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Pool enabled status updated successfully");
                    response.put("enabled", enabled);
                    sendJsonResponse(exchange, 200, response);
                } else {
                    sendResponse(exchange, 404, "Pool not found");
                }

            } catch (Exception e) {
                System.err.println("Error in SetPool1v1EnabledHandler: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }
}