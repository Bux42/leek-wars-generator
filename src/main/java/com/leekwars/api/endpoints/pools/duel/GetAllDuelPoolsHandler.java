package com.leekwars.api.endpoints.pools.duel;

import java.io.IOException;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.PoolDuelService;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.pool.categories.PoolDuel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class GetAllDuelPoolsHandler implements HttpHandler {
    private final PoolDuelService poolDuelService;

    public GetAllDuelPoolsHandler(PoolDuelService poolDuelService) {
        this.poolDuelService = poolDuelService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }
        try {
            List<PoolDuel> pools = poolDuelService.getAllPoolDuels();

            JSONObject response = new JSONObject();
            response.put("pools", pools);
            response.put("success", true);

            RequestUtils.sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("Error in ListPool1v1Handler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

}
