package com.leekwars.api.endpoints.leekscriptAis;

import com.sun.net.httpserver.HttpHandler;

import leekscript.compiler.AIFile;
import leekscript.compiler.IACompiler.AnalyzeResult;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.generator.Generator;
import com.leekwars.pool.leek.LeekManager;

public class AnalyzeLeekscriptAiFileHandler implements HttpHandler {
    private final Generator generator;

    public AnalyzeLeekscriptAiFileHandler(Generator generator) {
        this.generator = generator;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }

        try {
            JSONObject json = RequestUtils.readRequestBody(exchange);

            String aiFilePath = json.getString("aiFilePath");

            AIFile aiFile = LeekManager.ResolveAIFile(aiFilePath, generator);
            generator.setCache(false);
            AnalyzeResult result = generator.analyzeAI(aiFile, 0);
            generator.setCache(true);

            if (result.informations.size() > 0 && !result.success) {
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("success", false);
                errorResponse.put("errorsCount", result.informations.size());
                RequestUtils.sendJsonResponse(exchange, 200, errorResponse);
                return;
            }

            JSONObject response = new JSONObject();
            response.put("success", true);

            RequestUtils.sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            System.err.println("Error in AnalyzeLeekscriptAiHandler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}
