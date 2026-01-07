package com.leekwars.api.endpoints.pools.duel;

import java.io.IOException;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.LeekService;
import com.leekwars.api.mongo.services.PoolDuelService;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.pool.categories.PoolDuel;
import com.leekwars.pool.leek.Leek;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RemoveLeekFromPoolDuelHandler implements HttpHandler {
    private final PoolDuelService poolDuelService;
    private final LeekService leekService;

    public RemoveLeekFromPoolDuelHandler(PoolDuelService poolDuelService, LeekService leekService) {
        this.poolDuelService = poolDuelService;
        this.leekService = leekService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"DELETE".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }
        try {
            JSONObject json = RequestUtils.readRequestBody(exchange);

            String poolId = json.getString("poolId");
            String leekId = json.getString("leekId");

            if (poolId == null || poolId.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required field: poolId");
                return;
            }

            PoolDuel pool = poolDuelService.getPoolDuelById(poolId);
            if (pool == null) {
                RequestUtils.sendResponse(exchange, 404, "Pool not found");
                return;
            }

            if (leekId == null || leekId.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required field: leekId");
                return;
            }

            if (!pool.leekIds.contains(leekId)) {
                RequestUtils.sendResponse(exchange, 404, "Leek not found in pool");
                return;
            }

            // Remove leek from pool
            boolean success = poolDuelService.removeLeekIdFromPoolDuel(poolId, leekId);

            if (success) {
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("message", "Leek removed from pool successfully");
                RequestUtils.sendJsonResponse(exchange, 200, response);
            } else {
                RequestUtils.sendResponse(exchange, 404, "Pool not found");
            }

        } catch (Exception e) {
            System.err.println("Error in RemoveLeekFromPoolDuelHandler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}
