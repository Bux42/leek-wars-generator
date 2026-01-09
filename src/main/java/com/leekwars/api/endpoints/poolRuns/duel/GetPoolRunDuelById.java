package com.leekwars.api.endpoints.poolRuns.duel;

import java.io.IOException;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.PoolRunDuelService;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.pool.run.categories.PoolRunDuel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class GetPoolRunDuelById implements HttpHandler {
    private final PoolRunDuelService poolRunDuelService;

    public GetPoolRunDuelById(PoolRunDuelService poolRunDuelService) {
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
            String poolRunDuelId = RequestUtils.getQueryParam(query, "id");

            PoolRunDuel poolRunDuel = poolRunDuelService.getPoolRunDuelById(poolRunDuelId);

            JSONObject response = new JSONObject();
            response.put("poolRunDuel", poolRunDuel);
            response.put("success", true);

            RequestUtils.sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("Error in PoolRunDuelService: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}
