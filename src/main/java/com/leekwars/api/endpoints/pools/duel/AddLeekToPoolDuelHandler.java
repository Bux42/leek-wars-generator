package com.leekwars.api.endpoints.pools.duel;

import java.io.IOException;


import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.MongoDbManager;
import com.leekwars.api.utils.RequestUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class AddLeekToPoolDuelHandler implements HttpHandler {
    private final MongoDbManager mongoDbManager;

    public AddLeekToPoolDuelHandler(MongoDbManager mongoDbManager) {
        this.mongoDbManager = mongoDbManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }
        try {
            JSONObject json = RequestUtils.readRequestBody(exchange);
            // Check if MongoDB is connected
            if (mongoDbManager == null || !mongoDbManager.isConnected()) {
                RequestUtils.sendResponse(exchange, 503, "Database not available");
                return;
            }

            String poolId = json.getString("pool_id");
            String leekId = json.getString("leek_id");

            if (poolId == null || poolId.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required field: pool_id");
                return;
            }

            if (leekId == null || leekId.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required field: leek_id");
                return;
            }

            // Add leek to pool
            boolean success = mongoDbManager.addLeekToPoolDuel(poolId, leekId);

            if (success) {
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("message", "Leek added to pool successfully");
                RequestUtils.sendJsonResponse(exchange, 200, response);
            } else {
                RequestUtils.sendResponse(exchange, 404, "Pool not found");
            }

        } catch (Exception e) {
            System.err.println("Error in AddLeekToPool1v1Handler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}
