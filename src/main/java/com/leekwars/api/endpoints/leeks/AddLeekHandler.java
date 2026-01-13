package com.leekwars.api.endpoints.leeks;

import java.io.IOException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.LeekScriptAiService;
import com.leekwars.api.mongo.services.LeekService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.pool.leek.Leek;

public class AddLeekHandler implements HttpHandler {
    private final LeekService leekService;
    private final LeekScriptAiService leekScriptAiService;

    public AddLeekHandler(LeekService leekService, LeekScriptAiService leekScriptAiService) {
        this.leekService = leekService;
        this.leekScriptAiService = leekScriptAiService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }
        try {
            JSONObject json = RequestUtils.readRequestBody(exchange);

            // Deserialize JSON into Leek object using fromJson method
            Leek leek = Leek.fromJson(json);

            // check if we already have a snapshot of this AI saved
            if (!leekScriptAiService.leekscriptAiExistsByMergedAiCodeHash(leek.mergedCodeHash)) {
                // error, the AI snapshot with this mergedCodeHash does not exist
                System.out.println("AddLeekscriptAiHandler: AI snapshot not found for mergedCodeHash: " + leek.mergedCodeHash);

                // repond with 404 ai not found
                RequestUtils.sendResponse(exchange, 404, "AI snapshot not found for mergedCodeHash: " + leek.mergedCodeHash);
                return;
            }

            // Validate required fields
            if (leek.name == null || leek.name.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required field: name");
                return;
            }

            // Set elo to default value (100)
            leek.elo = 100;

            // Add leek to database
            String leekId = leekService.addLeek(leek);

            // Set the ID on the leek object
            leek.id = leekId;

            // Return the newly created leek as JSON
            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("leek", JSON.parseObject(JSON.toJSONString(leek)));

            System.out.println("Successfully added leek: " + leek.name + " with ID: " + leekId);
            RequestUtils.sendJsonResponse(exchange, 201, response);
        } catch (Exception e) {
            System.err.println("Error in AddLeekHandler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}
