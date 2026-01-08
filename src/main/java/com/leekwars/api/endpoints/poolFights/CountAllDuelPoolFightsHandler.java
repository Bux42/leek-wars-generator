package com.leekwars.api.endpoints.poolFights;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.PoolFightDuelService;
import com.leekwars.api.utils.RequestUtils;

public class CountAllDuelPoolFightsHandler implements HttpHandler {
    private final PoolFightDuelService poolFightDuelService;

    public CountAllDuelPoolFightsHandler(PoolFightDuelService poolFightDuelService) {
        this.poolFightDuelService = poolFightDuelService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }

        try {
            int count = poolFightDuelService.countAllPoolFights();

            JSONObject response = new JSONObject();
            response.put("count", count);
            response.put("success", true);
            
            RequestUtils.sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            System.err.println("Error in CountAllDuelPoolFightsHandler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}