package com.leekwars.api.endpoints.pools.duel;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.LeekScriptAiService;
import com.leekwars.api.mongo.services.LeekService;
import com.leekwars.api.mongo.services.PoolDuelService;
import com.leekwars.api.mongo.services.PoolRunDuelService;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.generator.Generator;
import com.leekwars.pool.PoolManager;
import com.leekwars.pool.categories.PoolDuel;
import com.leekwars.pool.code.GitInfos;
import com.leekwars.pool.code.CodeSnapshot;
import com.leekwars.pool.code.MergedCode;
import com.leekwars.pool.leek.Leek;
import com.leekwars.pool.leek.LeekManager;
import com.leekwars.pool.leek.PoolRunLeek;
import com.leekwars.pool.run.categories.PoolRunDuel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import leekscript.compiler.AIFile;

public class StartPoolDuelHandler implements HttpHandler {
    private final PoolDuelService poolDuelService;
    private final PoolRunDuelService poolRunDuelService;
    private final LeekService leekService;
    private final LeekScriptAiService leekScriptAiService;
    private final Generator generator;

    private final PoolManager poolManager;

    public StartPoolDuelHandler(PoolManager poolManager, LeekService leekService, PoolDuelService poolDuelService, PoolRunDuelService poolRunDuelService, LeekScriptAiService leekScriptAiService, Generator generator) {
        this.leekService = leekService;
        this.poolDuelService = poolDuelService;
        this.poolRunDuelService = poolRunDuelService;
        this.leekScriptAiService = leekScriptAiService;
        this.generator = generator;

        this.poolManager = poolManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }
        try {
            JSONObject json = RequestUtils.readRequestBody(exchange);

            String poolId = json.getString("id");
            if (poolId == null || poolId.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required field: id");
                return;
            }

            PoolDuel pool = poolDuelService.getPoolDuelById(poolId);

            if (pool == null) {
                RequestUtils.sendResponse(exchange, 404, "Pool not found");
                return;
            }

            // pool must have at least 2 leeks
            if (pool.leekIds == null || pool.leekIds.size() < 2) {
                RequestUtils.sendResponse(exchange, 400, "Pool must have at least 2 leeks");
                return;
            }

            // fetch all pool leeks from database
            List<Leek> poolDuelLeeks = leekService.getLeeksByIds(pool.leekIds);

            // convert to a PoolRunLeek (snapshot build, ai, git status etc at the start of
            // the pool)
            List<PoolRunLeek> poolRunLeeks = new ArrayList<>();

            for (Leek leek : poolDuelLeeks) {
                AIFile aiFile = LeekManager.ResolveAIFile(leek, generator);

                if (aiFile == null) {
                    RequestUtils.sendResponse(exchange, 404, "AI file not found for leek ID " + leek.id);
                    return;
                }

                // get merged AI code and its hash
                MergedCode aiMergedCode = LeekManager.GetLeekScriptMergedCode(leek, aiFile, generator);

                if (aiMergedCode == null) {
                    RequestUtils.sendResponse(exchange, 500, "Failed to get merged AI code for leek ID " + leek.id);
                    return;
                }

                // Create new leek snapshot with merged AI code hash
                PoolRunLeek poolRunLeek = new PoolRunLeek(leek, aiMergedCode.hash);

                // check if we already have a snapshot of this AI saved
                CodeSnapshot leekSnapshotAI = leekScriptAiService.getLeekAiByMergedAiCodeHash(aiMergedCode.hash);

                // we don't have this AI saved yet, create a new snapshot, with git info if
                // possible
                if (leekSnapshotAI == null) {
                    // try and populate git info from the AI file if possible
                    GitInfos gitInfos = LeekManager.TryGetGitInfos(leek);

                    leekSnapshotAI = new CodeSnapshot(aiMergedCode, gitInfos);

                    String newSnapshotId = leekScriptAiService.addLeekAi(leekSnapshotAI);
                    System.out.println("StartPoolDuelHandler: Created new AI snapshot with ID " + newSnapshotId + " for leek ID " + leek.id);
                }

                // PoolRunLeek poolRunLeek = LeekManager.CreatePoolRunLeek(leek,
                // poolManager.generator, mongoDbManager);
                poolRunLeeks.add(poolRunLeek);
            }

            PoolRunDuel poolRunDuel = new PoolRunDuel(pool, poolRunLeeks);
            // poolRunDuel.SetLeeks(poolRunLeeks);

            String newRunId = poolRunDuelService.addPoolRunDuel(poolRunDuel);
            poolRunDuel.id = newRunId;
            
            System.out.println("StartPoolDuelHandler: Created new PoolRunDuel with ID " + newRunId + " for PoolDuel ID " + pool.id);

            boolean startSuccess = poolManager.startPoolDuel(poolRunDuel);

            if (!startSuccess) {
                RequestUtils.sendResponse(exchange, 500, "Failed to create the pool run");
                return;
            }

            if (startSuccess) {
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("run_id", newRunId);
                RequestUtils.sendJsonResponse(exchange, 200, response);
            } else {
                RequestUtils.sendResponse(exchange, 500, "Failed to start the pool");
            }
        } catch (Exception e) {
            System.err.println("Error in StartPoolDuelHandler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

}
