
package com.leekwars.api.endpoints.leekscriptAis;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.LeekScriptAiService;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.pool.code.LeekscriptAI;

public class GetAllLeekscriptAisHandler implements HttpHandler {
    private final LeekScriptAiService leekScriptAiService;

    public GetAllLeekscriptAisHandler(LeekScriptAiService leekScriptAiService) {
        this.leekScriptAiService = leekScriptAiService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }

        try {
            String query = exchange.getRequestURI().getQuery();
            String removeCode = RequestUtils.getQueryParam(query, "removeCode");

            boolean shouldRemoveCode = "true".equalsIgnoreCase(removeCode);

            List<LeekscriptAI> leekscriptAi = leekScriptAiService.getAllLeekscriptAis(shouldRemoveCode);

            if (leekscriptAi == null) {
                RequestUtils.sendResponse(exchange, 404, "Leekscript AI not found");
                return;
            }

            JSONObject response = new JSONObject();

            response.put("leekscriptAis", leekscriptAi);
            response.put("success", true);

            RequestUtils.sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            System.err.println("Error in GetAllLeekscriptAisHandler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}
