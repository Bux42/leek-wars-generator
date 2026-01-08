package com.leekwars.api.endpoints.leeks;

import java.io.IOException;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.LeekService;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.pool.leek.Leek;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class GetLeekByIdHandler implements HttpHandler {
    private final LeekService leekService;

    public GetLeekByIdHandler(LeekService leekService) {
        this.leekService = leekService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }
        try {
            String query = exchange.getRequestURI().getQuery();
            String leekId = RequestUtils.getQueryParam(query, "id");

            if (leekId == null || leekId.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required parameter: id");
                return;
            }

            Leek leek = leekService.getLeekById(leekId);

            if (leek == null) {
                RequestUtils.sendResponse(exchange, 404, "Leek not found");
                return;
            }

            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("leek", leek);

            RequestUtils.sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            System.err.println("Error in GetLeekByIdHandler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}
