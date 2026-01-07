package com.leekwars.api.endpoints.pools.duel;

import java.io.IOException;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.PoolDuelService;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.pool.categories.PoolDuel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class AddPoolDuelHandler implements HttpHandler {
    private final PoolDuelService poolDuelService;

    public AddPoolDuelHandler(PoolDuelService poolDuelService) {
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

            String name = json.getString("name");

            if (name == null || name.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required fields: name");
                return;
            }

            // Deserialize JSON into PoolDuel object
            PoolDuel pool = PoolDuel.fromJson(json);

            // Add pool to database
            String poolId = poolDuelService.addPoolDuel(pool);

            if (poolId != null) {
                pool.id = poolId;

                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("pool", pool);

                System.out.println("Successfully added Duel pool: " + pool.name + " with ID: " + poolId);
                RequestUtils.sendJsonResponse(exchange, 201, response);
            } else {
                RequestUtils.sendResponse(exchange, 500, "Failed to add Duel pool to database");
            }

        } catch (Exception e) {
            System.err.println("Error in AddPoolDuelHandler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}
