package com.leekwars.api.endpoints.leeks;

import java.io.IOException;

import org.bson.Document;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.MongoDbManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.leekwars.api.utils.RequestUtils;

public class GetLeeksHandler implements HttpHandler {
    private final MongoDbManager mongoDbManager;

    public GetLeeksHandler(MongoDbManager mongoDbManager) {
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

            // Get all leeks from the database
            var leeksCollection = mongoDbManager.getCollection("leeks");
            JSONArray leeks = new JSONArray();

            for (Document doc : leeksCollection.find()) {
                // Convert MongoDB document to JSON
                String docJson = doc.toJson();
                JSONObject leekJson = JSON.parseObject(docJson);

                // Remove MongoDB's _id field and use our custom id field
                leekJson.remove("_id");

                leeks.add(leekJson);
            }

            JSONObject response = new JSONObject();
            response.put("leeks", leeks);
            response.put("success", true);

            RequestUtils.sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("Error in LeeksHandler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}
