package com.leekwars.api.endpoints.pools.duel;

import java.io.IOException;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.PoolDuelService;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.pool.categories.PoolDuel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class UpdatePoolDuelHandler implements HttpHandler {
    private final PoolDuelService poolDuelService;

    public UpdatePoolDuelHandler(PoolDuelService poolDuelService) {
        this.poolDuelService = poolDuelService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"PUT".equals(exchange.getRequestMethod())) {
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

            PoolDuel poolDuel = PoolDuel.fromJson(json);
            if (poolDuel == null) {
                RequestUtils.sendResponse(exchange, 400, "Invalid pool data");
                return;
            }


            // Update pool in database
            boolean success = poolDuelService.updatePoolDuel(poolId, poolDuel);

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
