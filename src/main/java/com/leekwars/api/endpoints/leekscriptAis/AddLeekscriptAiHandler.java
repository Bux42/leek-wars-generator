package com.leekwars.api.endpoints.leekscriptAis;

import com.sun.net.httpserver.HttpHandler;

import leekscript.compiler.AIFile;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.generator.Generator;
import com.leekwars.pool.code.GitInfos;
import com.leekwars.pool.code.LeekscriptAI;
import com.leekwars.pool.code.MergedCode;
import com.leekwars.pool.leek.LeekManager;
import com.leekwars.api.files.FileManager;
import com.leekwars.api.mongo.services.LeekScriptAiService;

public class AddLeekscriptAiHandler implements HttpHandler {
    private final Generator generator;
    private final LeekScriptAiService leekScriptAiService;

    public AddLeekscriptAiHandler(Generator generator, LeekScriptAiService leekScriptAiService) {
        this.generator = generator;
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

            String aiFilePath = json.getString("aiFilePath");

            if (aiFilePath == null || aiFilePath.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required field: aiFilePath");
                return;
            }

            String name = json.getString("name");

            if (name == null || name.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required field: name");
                return;
            }

            String description = json.getString("description");

            if (description == null) {
                description = "";
            }

            AIFile aiFile = LeekManager.ResolveAIFile(aiFilePath, generator);

            if (aiFile == null) {
                RequestUtils.sendResponse(exchange, 404, "AI file not found  at path: " + aiFilePath);
                return;
            }

            // get merged AI code and its hash
            MergedCode aiMergedCode = LeekManager.GetLeekScriptMergedCode(aiFile, generator);

            if (aiMergedCode == null) {
                RequestUtils.sendResponse(exchange, 500, "Failed to get merged AI code for AI path: " + aiFilePath);
                return;
            }

            // check if we already have a snapshot of this AI saved
            LeekscriptAI leekSnapshotAI = leekScriptAiService.getLeekscriptAiByMergedAiCodeHash(aiMergedCode.hash, true);

            // we don't have this AI saved yet, create a new snapshot, with git info if
            // possible
            if (leekSnapshotAI == null) {
                // save merged AI code to .merged_ais folder
                FileManager fileManager = new FileManager();
                fileManager.writeFile(".merged_ais/" + aiMergedCode.hash + ".leek", aiMergedCode.code);
                
                // try and populate git info from the AI file if possible
                GitInfos gitInfos = LeekManager.TryGetGitInfos(aiFilePath);

                leekSnapshotAI = new LeekscriptAI(aiMergedCode, gitInfos, name, description, aiFilePath);

                leekScriptAiService.addLeekscriptAi(leekSnapshotAI);
                System.out.println("AddLeekscriptAiHandler: Created new AI snapshot mergedCodeHash " + leekSnapshotAI.mergedCode.hash);

                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("mergedCodeHash", leekSnapshotAI.mergedCode.hash);
                RequestUtils.sendJsonResponse(exchange, 200, response);
            } else {
                System.out.println("AddLeekscriptAiHandler: AI snapshot already exists mergedCodeHash " + leekSnapshotAI.mergedCode.hash);

                JSONObject response = new JSONObject();
                response.put("success", false);
                response.put("mergedCodeHash", leekSnapshotAI.mergedCode.hash);
                response.put("message", "AI snapshot already exists");

                RequestUtils.sendJsonResponse(exchange, 200, response);
            }

        } catch (Exception e) {
            System.err.println("Error in AddLeekscriptAiHandler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

}
