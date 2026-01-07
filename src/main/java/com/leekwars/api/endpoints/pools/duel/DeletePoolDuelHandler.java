package com.leekwars.api.endpoints.pools.duel;

import java.io.IOException;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.PoolDuelService;
import com.leekwars.api.utils.RequestUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class DeletePoolDuelHandler implements HttpHandler {
    private final PoolDuelService poolDuelService;

    public DeletePoolDuelHandler(PoolDuelService poolDuelService) {
        this.poolDuelService = poolDuelService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"DELETE".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }
        try {
            JSONObject json = RequestUtils.readRequestBody(exchange);

            // TODO: check if there are any PoolDuelRuns associated with this pool and
            // prevent deletion if so
            // Delete all associated PoolDuelRuns first, with their fights etc

            String poolId = json.getString("id");
            if (poolId == null || poolId.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required field: id");
                return;
            }

            // Delete pool from database
            boolean success = poolDuelService.deletePoolDuel(poolId);

            if (success) {
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("message", "Pool deleted successfully");
                RequestUtils.sendJsonResponse(exchange, 200, response);
            } else {
                RequestUtils.sendResponse(exchange, 404, "Pool not found");
            }

        } catch (Exception e) {
            System.err.println("Error in DeletePool1v1Handler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

}
