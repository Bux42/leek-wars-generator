package com.leekwars.api.endpoints.leeks;

import java.io.IOException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.LeekService;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.pool.leek.Leek;
import com.sun.net.httpserver.HttpHandler;

public class UpdateLeekHandler implements HttpHandler {
    private final LeekService leekService;

    public UpdateLeekHandler(LeekService leekService) {
        this.leekService = leekService;
    }

    @Override
    public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        // Implementation for updating a leek goes here
        if (!"PUT".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }
        try {
            JSONObject json = RequestUtils.readRequestBody(exchange);

            // Deserialize JSON into Leek object using fromJson method
            Leek leek = Leek.fromJson(json);

            // Validate required fields
            if (leek.name == null || leek.name.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required field: name");
                return;
            }

            // Update leek in database
            boolean updateSuccesss = leekService.updateLeek(leek);

            if (!updateSuccesss) {
                RequestUtils.sendResponse(exchange, 404, "Leek not found with ID: " + leek.id);
                return;
            }

            // Return the newly created leek as JSON
            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("leek", JSON.parseObject(JSON.toJSONString(leek)));

            System.out.println("Successfully updated leek: " + leek.name + " with ID: " + leek.id);
            RequestUtils.sendJsonResponse(exchange, 201, response);
        } catch (Exception e) {
            System.err.println("Error in UpdateLeekHandler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}
