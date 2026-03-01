package com.leekwars;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.leekwars.generator.Generator;
import com.leekwars.generator.Log;
import com.leekwars.generator.outcome.Outcome;
import com.leekwars.generator.scenario.Scenario;
import com.leekwars.socket.SocketScenario;
import com.leekwars.generator.test.LocalDbFileSystem;
import com.leekwars.generator.test.LocalDbRegisterManager;
import com.leekwars.generator.test.LocalTrophyManager;

import leekscript.compiler.IACompiler.AnalyzeResult;
import leekscript.compiler.LeekScript;
import leekscript.compiler.resolver.NativeFileSystem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;

public class Socket {

    private static final String TAG = Socket.class.getSimpleName();
    private static final JsonMapper JSON = new JsonMapper();

    public static void main(String[] args) {
        String host = "0.0.0.0";
        int port = 9090;
        boolean nocache = false;
        boolean db_resolver = false;
        boolean verbose = false;

        for (String arg : args) {
            if (arg.startsWith("--")) {
                switch (arg.substring(2)) {
                    case "nocache":
                        nocache = true;
                        break;
                    case "dbresolver":
                        db_resolver = true;
                        break;
                    case "verbose":
                        verbose = true;
                        break;
                }
                if (arg.startsWith("--port=")) {
                    port = Integer.parseInt(arg.substring("--port=".length()));
                } else if (arg.startsWith("--host=")) {
                    host = arg.substring("--host=".length());
                }
            }
        }

        Log.enable(verbose);
        Log.i(TAG, "Generator socket server v1");

        if (db_resolver) {
            LeekScript.setFileSystem(new LocalDbFileSystem());
        } else {
            LeekScript.setFileSystem(new NativeFileSystem());
        }

        Generator generator = new Generator();
        generator.setCache(!nocache);

        startServer(host, port, generator);
    }

    private static void startServer(String host, int port, Generator generator) {
        var pool = Executors.newCachedThreadPool();
        try (ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName(host))) {
            Log.i(TAG, "Listening on " + host + ":" + port + " (JSON lines protocol)");
            while (true) {
                java.net.Socket client = server.accept();
                pool.submit(() -> handleClient(client, generator));
            }
        } catch (IOException e) {
            Log.e(TAG, "Socket server failed to start: " + e.getMessage());
            shutdownPool(pool);
        }
    }

    private static void handleClient(java.net.Socket client, Generator generator) {
        String remote = client.getRemoteSocketAddress().toString();
        Log.i(TAG, "Client connected: " + remote);

        try (
                client;
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(client.getOutputStream(), true, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                ObjectNode response = processRequest(line, generator);
                writer.println(response.toString());
            }
        } catch (IOException e) {
            Log.e(TAG, "Client I/O error for " + remote + ": " + e.getMessage());
        }

        Log.i(TAG, "Client disconnected: " + remote);
    }

    private static ObjectNode processRequest(String rawRequest, Generator generator) {
        JsonNode request;
        try {
            request = JSON.readTree(rawRequest);
        } catch (Exception e) {
            return error("invalid_json", "Malformed JSON request");
        }

        if (request == null || !request.isObject()) {
            return error("invalid_request", "Request must be a JSON object");
        }

        String type = request.path("type").stringValue("");
        if (type.isEmpty()) {
            return error("invalid_request", "Missing field: type");
        }

        return switch (type) {
            case "health" -> success("health", JSON.createObjectNode().put("status", "ok"));
            case "analyze" -> handleAnalyze(request, generator);
            case "scenario" -> handleScenario(request, generator);
            case "scenario_winner" -> handleScenarioWinner(request, generator);
            default -> error("unknown_type", "Unsupported request type: " + type);
        };
    }

    private static ObjectNode handleAnalyze(JsonNode request, Generator generator) {
        String file = request.path("file").stringValue("");
        if (file.isEmpty()) {
            return error("invalid_request", "Missing field: file");
        }

        try {
            var ai = LeekScript.getFileSystem().getRoot().resolve(file);
            AnalyzeResult result = generator.analyzeAI(ai, 0);
            return success("analyze", JSON.valueToTree(result.informations));
        } catch (FileNotFoundException e) {
            return error("file_not_found", "File not found: " + file);
        } catch (Exception e) {
            return error("analyze_failed", e.getMessage());
        }
    }

    private static ObjectNode handleScenarioWinner(JsonNode request, Generator generator) {
        System.out.println("Received scenario winner request: " + request.toString());
        if (!request.has("scenario") || !request.get("scenario").isObject()) {
            return error("invalid_request", "Missing or invalid field: scenario");
        }

        boolean useCache = request.path("useCache").asBoolean(true);

        Scenario scenario;
        try {
            scenario = SocketScenario.fromJson(request);
        } catch (IllegalArgumentException e) {
            return error("invalid_request", e.getMessage());
        } catch (Exception e) {
            return error("invalid_request", "Failed to parse scenario payload");
        }

        try {
            Outcome outcome;
            synchronized (generator) {
                generator.setCache(useCache);
                outcome = generator.runScenario(scenario, null, new LocalDbRegisterManager(),
                        new LocalTrophyManager());
            }
            System.out.println("Winner: " + outcome.winner);
            ObjectNode response = JSON.createObjectNode();
            response.put("winner", outcome.winner);
            return success("scenario", response);
        } catch (Exception e) {
            return error("scenario_failed", e.getMessage());
        }
    }

    private static ObjectNode handleScenario(JsonNode request, Generator generator) {
        System.out.println("Received scenario request: " + request.toString());
        if (!request.has("scenario") || !request.get("scenario").isObject()) {
            return error("invalid_request", "Missing or invalid field: scenario");
        }

        boolean useCache = request.path("useCache").asBoolean(true);

        Scenario scenario;
        try {
            scenario = SocketScenario.fromJson(request);
        } catch (IllegalArgumentException e) {
            return error("invalid_request", e.getMessage());
        } catch (Exception e) {
            return error("invalid_request", "Failed to parse scenario payload");
        }

        try {
            Outcome outcome;
            synchronized (generator) {
                generator.setCache(useCache);
                outcome = generator.runScenario(scenario, null, new LocalDbRegisterManager(),
                        new LocalTrophyManager());
            }
            System.out.println("Winner: " + outcome.winner);
            return success("scenario", outcome.toJson());
        } catch (Exception e) {
            return error("scenario_failed", e.getMessage());
        }
    }

    private static ObjectNode success(String type, JsonNode data) {
        ObjectNode response = JSON.createObjectNode();
        response.put("ok", true);
        response.put("type", type);
        response.set("data", data);
        return response;
    }

    private static ObjectNode error(String code, String message) {
        ObjectNode response = JSON.createObjectNode();
        response.put("ok", false);
        response.put("error", code);
        response.put("message", message == null ? "Unknown error" : message);
        return response;
    }

    private static void shutdownPool(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
