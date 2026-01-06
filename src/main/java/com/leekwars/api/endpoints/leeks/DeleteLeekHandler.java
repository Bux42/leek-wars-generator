package com.leekwars.api.endpoints.leeks;

import java.io.IOException;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.LeekService;
import com.leekwars.api.utils.RequestUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class DeleteLeekHandler  implements HttpHandler {
    private final LeekService leekService;

    public DeleteLeekHandler(LeekService leekService) {
        this.leekService = leekService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"DELETE".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }
        try {
            JSONObject json = RequestUtils.readRequestBody(exchange);

            String leekId = json.getString("id");

            if (leekId == null || leekId.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required field: id");
                return;
            }
            
            boolean success = leekService.deleteLeek(leekId);

            if (!success) {
                RequestUtils.sendResponse(exchange, 404, "Leek not found or could not be deleted");
                return;
            }

            JSONObject response = new JSONObject();
            response.put("success", true);
            RequestUtils.sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            System.err.println("Error in DeleteLeekHandler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
}
