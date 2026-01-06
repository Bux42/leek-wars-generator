package com.leekwars.api.endpoints.pools.duel;

import java.io.IOException;

import org.bson.Document;
import java.util.ArrayList;
import java.util.List;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.MongoDbManager;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.pool.PoolManager;
import com.leekwars.pool.categories.PoolDuel;
import com.leekwars.pool.leek.Leek;
import com.leekwars.pool.leek.LeekManager;
import com.leekwars.pool.leek.PoolRunLeek;
import com.leekwars.pool.run.categories.PoolRunDuel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class StartPoolDuelHandler implements HttpHandler {
    private final MongoDbManager mongoDbManager;
    private final PoolManager poolManager;

    public StartPoolDuelHandler(MongoDbManager mongoDbManager, PoolManager poolManager) {
        this.mongoDbManager = mongoDbManager;
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

            // Check if PoolManager is available
            if (poolManager == null) {
                RequestUtils.sendResponse(exchange, 503, "PoolManager not available");
                return;
            }

            String poolId = json.getString("id");
            if (poolId == null || poolId.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required field: id");
                return;
            }

            // check if pool is already running
            if (poolManager.isPoolRunning(poolId)) {
                RequestUtils.sendResponse(exchange, 400, "Pool is already running");
                return;
            }

            // get pool from database to check if it exists
            Document poolDoc = mongoDbManager.getPoolDuelById(poolId);

            if (poolDoc == null) {
                RequestUtils.sendResponse(exchange, 404, "Pool not found");
                return;
            }

            // get pool object
            PoolDuel pool = PoolDuel.fromJson(JSON.parseObject(poolDoc.toJson()));

            // get all leek_ids from poolDoc
            JSONObject poolJson = JSON.parseObject(poolDoc.toJson());
            JSONArray leekIds = poolJson.getJSONArray("leek_ids");

            // pool must have at least 2 leeks
            if (leekIds == null || leekIds.size() < 2) {
                RequestUtils.sendResponse(exchange, 400, "Pool must have at least 2 leeks");
                return;
            }

            List<String> leekIdsList = new ArrayList<>();
            for (int i = 0; i < leekIds.size(); i++) {
                leekIdsList.add(leekIds.getString(i));
            }
            // fetch all leeks from database
            List<Leek> poolDuelLeeks = mongoDbManager.getLeeksByIds(leekIdsList);

            // convert to a PoolRunLeek (snapshot build, ai, git status etc at the start of
            // the pool)
            List<PoolRunLeek> poolRunLeeks = new ArrayList<>();
            for (Leek leek : poolDuelLeeks) {
                PoolRunLeek poolRunLeek = LeekManager.CreatePoolRunLeek(leek, poolManager.generator, mongoDbManager);
                poolRunLeeks.add(poolRunLeek);
            }

            PoolRunDuel poolRunDuel = new PoolRunDuel(pool);
            // store snapshot of leeks at the start of the pool
            poolRunDuel.SetLeeks(poolRunLeeks);

            boolean startSuccess = poolRunDuel.start(mongoDbManager);

            if (!startSuccess) {
                RequestUtils.sendResponse(exchange, 500, "Failed to create the pool run");
                return;
            }

            poolManager.startPoolDuel(poolRunDuel);

            if (startSuccess) {
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("run_id", poolRunDuel.id);
                RequestUtils.sendJsonResponse(exchange, 200, response);
            } else {
                RequestUtils.sendResponse(exchange, 500, "Failed to start the pool");
            }
        } catch (Exception e) {
            System.err.println("Error in StartPool1v1Handler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

}
