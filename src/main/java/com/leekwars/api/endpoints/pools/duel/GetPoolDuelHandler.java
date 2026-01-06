package com.leekwars.api.endpoints.pools.duel;

import java.io.IOException;

import org.bson.Document;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.MongoDbManager;
import com.leekwars.api.utils.RequestUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class GetPoolDuelHandler implements HttpHandler {
    private final MongoDbManager mongoDbManager;

    public GetPoolDuelHandler(MongoDbManager mongoDbManager) {
        this.mongoDbManager = mongoDbManager;
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

            Document poolDuelDocument = mongoDbManager.getPoolDuelById(poolId);

            if (poolDuelDocument == null) {
                RequestUtils.sendResponse(exchange, 404, "Pool not found");
                return;
            }

            JSONObject response = JSONObject.parseObject(poolDuelDocument.toJson());
            response.remove("_id");
            RequestUtils.sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            System.err.println("Error in ListPool1v1Handler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

}
