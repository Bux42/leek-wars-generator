package com.leekwars.api.endpoints.pools;

import java.io.IOException;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.utils.RequestUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

// Pools endpoints
public class GetPoolsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }

        try {
            // For demonstration, we return a static list of pools
            JSONArray pools = new JSONArray();
            pools.add("Pool A");
            pools.add("Pool B");
            pools.add("Pool C");

            JSONObject response = new JSONObject();
            response.put("pools", pools);
            response.put("success", true);

            RequestUtils.sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("Error in PoolsHandler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}