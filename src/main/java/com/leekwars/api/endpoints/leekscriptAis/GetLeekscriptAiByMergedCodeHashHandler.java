package com.leekwars.api.endpoints.leekscriptAis;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.LeekScriptAiService;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.pool.code.LeekscriptAI;

public class GetLeekscriptAiByMergedCodeHashHandler implements HttpHandler {
    private final LeekScriptAiService leekScriptAiService;

    public GetLeekscriptAiByMergedCodeHashHandler(LeekScriptAiService leekScriptAiService) {
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
            String mergedCodeHash = RequestUtils.getQueryParam(query, "mergedCodeHash");
            String removeCode = RequestUtils.getQueryParam(query, "removeCode");

            boolean shouldRemoveCode = "true".equalsIgnoreCase(removeCode);

            if (mergedCodeHash == null || mergedCodeHash.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required field: mergedCodeHash");
                return;
            }

            LeekscriptAI leekscriptAi = leekScriptAiService.getLeekscriptAiByMergedAiCodeHash(mergedCodeHash, shouldRemoveCode);

            if (leekscriptAi == null) {
                RequestUtils.sendResponse(exchange, 404, "Leekscript AI not found");
                return;
            }

            // remove diff info as it breaks json serialization
            leekscriptAi.gitInfos.diffOutput = null;

            JSONObject response = new JSONObject();

            response.put("codeSnapshot", leekscriptAi);
            response.put("success", true);

            RequestUtils.sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            System.err.println("Error in GetLeekscriptAiByIdHandler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}