package com.leekwars.api.endpoints.pools.duel;

import java.io.IOException;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.PoolDuelService;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.pool.categories.PoolDuel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class GetPoolDuelHandler implements HttpHandler {
    private final PoolDuelService poolDuelService;

    public GetPoolDuelHandler(PoolDuelService poolDuelService) {
        this.poolDuelService = poolDuelService;
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

            PoolDuel poolDuel = poolDuelService.getPoolDuelById(poolId);

            if (poolDuel == null) {
                RequestUtils.sendResponse(exchange, 404, "Pool not found");
                return;
            }

            JSONObject response = poolDuel.toJson();
            RequestUtils.sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            System.err.println("Error in ListPool1v1Handler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

}
