package com.leekwars.api.endpoints.pools.duel;

import java.io.IOException;

import org.bson.Document;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.MongoDbManager;
import com.leekwars.api.utils.RequestUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class UpdatePoolDuelHandler implements HttpHandler {
    private final MongoDbManager mongoDbManager;

    public UpdatePoolDuelHandler(MongoDbManager mongoDbManager) {
        this.mongoDbManager = mongoDbManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"PUT".equals(exchange.getRequestMethod())) {
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

            String poolId = json.getString("id");
            if (poolId == null || poolId.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required field: id");
                return;
            }

            // Remove id from updates
            json.remove("id");

            // Convert to Document
            Document updates = Document.parse(json.toJSONString());

            // Update pool in database
            boolean success = mongoDbManager.updatePoolDuel(poolId, updates);

            if (success) {
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("message", "Pool updated successfully");
                RequestUtils.sendJsonResponse(exchange, 200, response);
            } else {
                RequestUtils.sendResponse(exchange, 404, "Pool not found");
            }

        } catch (Exception e) {
            System.err.println("Error in UpdatePool1v1Handler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}
