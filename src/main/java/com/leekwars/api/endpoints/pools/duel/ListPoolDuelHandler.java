package com.leekwars.api.endpoints.pools.duel;

import java.io.IOException;

import org.bson.Document;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.MongoDbManager;
import com.leekwars.api.utils.RequestUtils;
import com.mongodb.client.MongoCollection;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ListPoolDuelHandler implements HttpHandler {
    private final MongoDbManager mongoDbManager;

    public ListPoolDuelHandler(MongoDbManager mongoDbManager) {
        this.mongoDbManager = mongoDbManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }
        try {
            // Check if MongoDB is connected
            if (mongoDbManager == null || !mongoDbManager.isConnected()) {
                RequestUtils.sendResponse(exchange, 503, "Database not available");
                return;
            }

            MongoCollection<Document> poolDuelCollection = mongoDbManager.getPoolDuelCollection();
            if (poolDuelCollection == null) {
                RequestUtils.sendResponse(exchange, 500, "Failed to get pool duel collection");
                return;
            }

            JSONObject response = new JSONObject();
            JSONArray poolsArray = new JSONArray();

            for (Document doc : poolDuelCollection.find()) {
                // Convert MongoDB document to JSON
                String docJson = doc.toJson();
                JSONObject poolJson = JSON.parseObject(docJson);

                // Remove MongoDB's _id field
                poolJson.remove("_id");

                poolsArray.add(poolJson);
            }

            response.put("pools", poolsArray);
            response.put("success", true);

            RequestUtils.sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("Error in ListPool1v1Handler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

}
