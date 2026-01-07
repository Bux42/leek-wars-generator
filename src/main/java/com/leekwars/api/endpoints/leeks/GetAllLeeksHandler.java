package com.leekwars.api.endpoints.leeks;

import java.io.IOException;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.LeekService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.pool.leek.Leek;

public class GetAllLeeksHandler implements HttpHandler {
    private final LeekService leekService;

    public GetAllLeeksHandler(LeekService leekService) {
        this.leekService = leekService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }
        try {
            List<Leek> leeks = leekService.getAllLeeks();

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
