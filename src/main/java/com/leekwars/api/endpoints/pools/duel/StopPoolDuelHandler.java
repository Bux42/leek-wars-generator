package com.leekwars.api.endpoints.pools.duel;

import java.io.IOException;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.MongoDbManager;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.pool.PoolManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class StopPoolDuelHandler implements HttpHandler {
    private final MongoDbManager mongoDbManager;
    private final PoolManager poolManager;

    public StopPoolDuelHandler(MongoDbManager mongoDbManager, PoolManager poolManager) {
        this.mongoDbManager = mongoDbManager;
        this.poolManager = poolManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }
        try {
            JSONObject json = RequestUtils.readRequestBody(exchange);

            String poolId = json.getString("id");
            if (poolId == null || poolId.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required field: id");
                return;
            }

            // Stop the pool
            boolean success = poolManager.stopPool(poolId);

            if (success) {
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("message", "Pool stopped successfully");
                RequestUtils.sendJsonResponse(exchange, 200, response);
            } else {
                RequestUtils.sendResponse(exchange, 400, "Pool is not running");
            }

        } catch (Exception e) {
            System.err.println("Error in StopPool1v1Handler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

}
