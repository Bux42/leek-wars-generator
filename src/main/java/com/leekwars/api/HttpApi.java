package com.leekwars.api;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.endpoints.leeks.AddLeekHandler;
import com.leekwars.api.endpoints.leeks.DeleteLeekHandler;
import com.leekwars.api.endpoints.leeks.GetAllLeeksHandler;
import com.leekwars.api.endpoints.leeks.GetLeekByIdHandler;
import com.leekwars.api.endpoints.leeks.UpdateLeekHandler;
import com.leekwars.api.endpoints.poolFights.duel.CountAllDuelPoolFightsByPoolRunIdHandler;
import com.leekwars.api.endpoints.poolFights.duel.CountAllDuelPoolFightsHandler;
import com.leekwars.api.endpoints.poolRuns.duel.GetAllDuelPoolRunsHandler;
import com.leekwars.api.endpoints.poolRuns.duel.GetPoolRunDuelById;
import com.leekwars.api.endpoints.poolRuns.duel.GetPoolRunDuelByPoolId;
import com.leekwars.api.endpoints.pools.GetPoolsHandler;
import com.leekwars.api.endpoints.pools.duel.AddLeekToPoolDuelHandler;
import com.leekwars.api.endpoints.pools.duel.AddPoolDuelHandler;
import com.leekwars.api.endpoints.pools.duel.DeletePoolDuelHandler;
import com.leekwars.api.endpoints.pools.duel.GetPoolDuelHandler;
import com.leekwars.api.endpoints.pools.duel.RemoveLeekFromPoolDuelHandler;
import com.leekwars.api.endpoints.pools.duel.GetAllDuelPoolsHandler;
import com.leekwars.api.endpoints.pools.duel.StartPoolDuelHandler;
import com.leekwars.api.endpoints.pools.duel.StopPoolDuelHandler;
import com.leekwars.api.endpoints.pools.duel.UpdatePoolDuelHandler;
import com.leekwars.api.files.FileManager;
import com.leekwars.api.files.FileManager.FileInfo;
import com.leekwars.api.middleware.LoggingHandler;
import com.leekwars.api.middleware.MongoHandler;
import com.leekwars.api.mongo.config.MongoClientProvider;
import com.leekwars.api.mongo.repositories.LeekAiRepository;
import com.leekwars.api.mongo.repositories.LeekRepository;
import com.leekwars.api.mongo.repositories.PoolDuelRepository;
import com.leekwars.api.mongo.repositories.PoolFightDuelRepository;
import com.leekwars.api.mongo.repositories.PoolRunDuelRepository;
import com.leekwars.api.mongo.services.LeekScriptAiService;
import com.leekwars.api.mongo.services.LeekService;
import com.leekwars.api.mongo.services.PoolDuelService;
import com.leekwars.api.mongo.services.PoolFightDuelService;
import com.leekwars.api.mongo.services.PoolRunDuelService;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.generator.Generator;
import com.leekwars.pool.PoolManager;

import leekscript.compiler.AIFile;
import leekscript.compiler.IACompiler.AnalyzeResult;
import leekscript.compiler.LeekScript;
import leekscript.compiler.resolver.NativeFileSystem;

public class HttpApi {
    private static Generator generator;
    private static final int DEFAULT_PORT = 8080;
    private static int port = DEFAULT_PORT;
    private static FileManager fileManager = new FileManager();
    private static PoolManager poolManager;

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

        MongoClientProvider mongoClientProvider = new MongoClientProvider("mongodb://localhost:27017", "leekwars");

        LeekRepository leekRepository = new LeekRepository(mongoClientProvider);
        LeekService leekService = new LeekService(leekRepository);

        PoolDuelRepository poolDuelRepository = new PoolDuelRepository(mongoClientProvider);
        PoolDuelService poolDuelService = new PoolDuelService(poolDuelRepository);

        PoolRunDuelRepository poolRunDuelRepository = new PoolRunDuelRepository(mongoClientProvider);
        PoolRunDuelService poolRunDuelService = new PoolRunDuelService(poolRunDuelRepository);

        PoolFightDuelRepository poolFightDuelRepository = new PoolFightDuelRepository(mongoClientProvider);
        PoolFightDuelService poolFightDuelService = new PoolFightDuelService(poolFightDuelRepository);

        LeekAiRepository leekAiRepository = new LeekAiRepository(mongoClientProvider);
        LeekScriptAiService leekScriptAiService = new LeekScriptAiService(leekAiRepository);

        // Initialize PoolManager
        poolManager = new PoolManager(poolRunDuelService, poolFightDuelService);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Ping endpoint
        server.createContext("/", new LoggingHandler(new RootHandler()));

        /* VSCODE EXTENSION ENDPOINTS */

        // Analyze AI code and return analysis endpoint
        server.createContext("/api/ai/analyze-file", new LoggingHandler(new AiAnalyzeFileHandler()));
        // CodeDefinition enpoints
        server.createContext("/api/get-definitions", new LoggingHandler(new CodeDefinitionHandler()));

        /* ELECTRON APP ENDPOINTS */

        // All pools
        server.createContext("/api/pools/get", new LoggingHandler(new GetPoolsHandler()));

        // Pool duel endpoints
        server.createContext("/api/pools/duel/get-all", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new GetAllDuelPoolsHandler(poolDuelService))));
        server.createContext("/api/pools/duel/add", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new AddPoolDuelHandler(poolDuelService))));
        server.createContext("/api/pools/duel/get", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new GetPoolDuelHandler(poolDuelService))));
        server.createContext("/api/pools/duel/update", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new UpdatePoolDuelHandler(poolDuelService))));
        server.createContext("/api/pools/duel/delete", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new DeletePoolDuelHandler(poolDuelService))));
        server.createContext("/api/pools/duel/add-leek", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new AddLeekToPoolDuelHandler(poolDuelService, leekService))));
        server.createContext("/api/pools/duel/remove-leek", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new RemoveLeekFromPoolDuelHandler(poolDuelService))));
        server.createContext("/api/pools/duel/start", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new StartPoolDuelHandler(poolManager, leekService, poolDuelService, poolRunDuelService, leekScriptAiService, generator))));
        server.createContext("/api/pools/duel/stop", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new StopPoolDuelHandler(poolManager))));

        // Pool duel runs endpoints
        server.createContext("/api/pool-runs/duel/get-all", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new GetAllDuelPoolRunsHandler(poolRunDuelService))));
        server.createContext("/api/pool-runs/duel/get-by-id", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new GetPoolRunDuelById(poolRunDuelService))));
        server.createContext("/api/pool-runs/duel/get-by-pool-id", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new GetPoolRunDuelByPoolId(poolRunDuelService))));

        // Pool duel fights endpoints
        server.createContext("/api/pool-fights/duel/count-all", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new CountAllDuelPoolFightsHandler(poolFightDuelService))));
        server.createContext("/api/pool-fights/duel/count-by-pool-run-id", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new CountAllDuelPoolFightsByPoolRunIdHandler(poolFightDuelService))));
                
        // Leeks
        server.createContext("/api/leeks/get-all", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new GetAllLeeksHandler(leekService))));
        server.createContext("/api/leeks/add", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new AddLeekHandler(leekService))));
        server.createContext("/api/leeks/delete", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new DeleteLeekHandler(leekService))));
        server.createContext("/api/leeks/update", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new UpdateLeekHandler(leekService))));
        server.createContext("/api/leeks/get-by-id", new LoggingHandler(
                new MongoHandler(mongoClientProvider, new GetLeekByIdHandler(leekService))));

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

    // Root handler
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "LeekScript Code Analysis Server is running";
            RequestUtils.sendResponse(exchange, 200, response);
        }
    }

    // AI analyze file handler - analyzes a file from disk using NativeFileSystem
    static class AiAnalyzeFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                RequestUtils.sendResponse(exchange, 405, "Method not allowed");
                return;
            }

            try {
                JSONObject json = RequestUtils.readRequestBody(exchange);
                String filePath = json.getString("file_path");

                if (filePath == null || filePath.isEmpty()) {
                    RequestUtils.sendResponse(exchange, 400, "Missing file_path parameter");
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

                    RequestUtils.sendJsonResponse(exchange, 200, response);

                } catch (FileNotFoundException e) {
                    System.err.println("File not found: " + filePath);
                    RequestUtils.sendResponse(exchange, 404, "File not found: " + filePath);
                }

            } catch (Exception e) {
                System.err.println("Error in AiAnalyzeFileHandler: " + e.getMessage());
                e.printStackTrace();
                RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
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
                RequestUtils.sendResponse(exchange, 405, "Method not allowed");
                return;
            }

            try {
                JSONObject json = RequestUtils.readRequestBody(exchange);

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

                    RequestUtils.sendJsonResponse(exchange, 200, result);
                } catch (FileNotFoundException e) {
                    System.err.println("File not found for CodeDefinition: " + file_path);
                    RequestUtils.sendResponse(exchange, 404, "File not found: " + file_path);
                    return;
                }

            } catch (Exception e) {
                System.err.println("Error in CodeDefinitionHandler: " + e.getMessage());
                e.printStackTrace();
                RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }

    // ----------- FILES ENDPOINTS ----------------

    // File list handler
    static class FileListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                RequestUtils.sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            try {
                JSONObject json = RequestUtils.readRequestBody(exchange);
                String directoryPath = json.getString("directory_path");

                if (directoryPath == null || directoryPath.isEmpty()) {
                    RequestUtils.sendResponse(exchange, 400, "Missing directory_path parameter");
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

                RequestUtils.sendJsonResponse(exchange, 200, response);

            } catch (Exception e) {
                System.err.println("Error in FileListHandler: " + e.getMessage());
                e.printStackTrace();
                RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }

    // Reset to root directory handler
    static class FileResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                RequestUtils.sendResponse(exchange, 405, "Method not allowed");
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

                RequestUtils.sendJsonResponse(exchange, 200, response);
            } catch (Exception e) {
                System.err.println("Error in FileResetHandler: " + e.getMessage());
                e.printStackTrace();
                RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }
}