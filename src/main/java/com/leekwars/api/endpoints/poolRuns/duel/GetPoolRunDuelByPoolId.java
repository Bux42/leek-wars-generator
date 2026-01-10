package com.leekwars.api.endpoints.poolRuns.duel;

import java.io.IOException;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.PoolRunDuelService;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.pool.run.categories.PoolRunDuel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class GetPoolRunDuelByPoolId implements HttpHandler {
    private final PoolRunDuelService poolRunDuelService;

    public GetPoolRunDuelByPoolId(PoolRunDuelService poolRunDuelService) {
        this.poolRunDuelService = poolRunDuelService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }
        try {
            String query = exchange.getRequestURI().getQuery();
            String poolId = RequestUtils.getQueryParam(query, "poolId");

            List<PoolRunDuel> poolRunDuels = poolRunDuelService.getAllPoolRunDuelByPoolId(poolId).reversed();

            JSONObject response = new JSONObject();
            response.put("poolRunDuels", poolRunDuels);
            response.put("success", true);

            RequestUtils.sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("Error in GetPoolRunDuelByPoolId: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
}
