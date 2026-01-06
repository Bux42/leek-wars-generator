package com.leekwars.api.endpoints.leeks;

import java.io.IOException;

import org.bson.Document;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.MongoDbManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.pool.leek.Leek;

public class AddLeekHandler implements HttpHandler {
    private final MongoDbManager mongoDbManager;

    public AddLeekHandler(MongoDbManager mongoDbManager) {
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

            // Check if MongoDB is connected
            if (mongoDbManager == null || !mongoDbManager.isConnected()) {
                RequestUtils.sendResponse(exchange, 503, "Database not available");
                return;
            }

            // Deserialize JSON into Leek object using fromJson method
            Leek leek = Leek.fromJson(json);

            // Validate required fields
            if (leek.name == null || leek.name.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required field: name");
                return;
            }

            // Set elo to default value (100)
            leek.elo = 100;

            // Convert the entire Leek object to JSON and then to MongoDB Document
            String leekJson = JSON.toJSONString(leek);
            Document leekData = Document.parse(leekJson);

            // Add leek to database
            String leekId = mongoDbManager.addLeek(leekData);

            if (leekId != null) {
                // Set the ID on the leek object
                leek.id = leekId;

                // Return the newly created leek as JSON
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("leek", JSON.parseObject(JSON.toJSONString(leek)));

                System.out.println("Successfully added leek: " + leek.name + " with ID: " + leekId);
                RequestUtils.sendJsonResponse(exchange, 201, response);
            } else {
                RequestUtils.sendResponse(exchange, 500, "Failed to add leek to database");
            }

        } catch (Exception e) {
            System.err.println("Error in AddLeekHandler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}
