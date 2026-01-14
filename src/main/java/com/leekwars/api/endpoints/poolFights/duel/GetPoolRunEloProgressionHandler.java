package com.leekwars.api.endpoints.poolFights.duel;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.services.PoolFightDuelService;
import com.leekwars.api.mongo.services.PoolRunDuelService;
import com.leekwars.api.utils.RequestUtils;
import com.leekwars.pool.run.categories.PoolRunDuel;
import com.leekwars.pool.run.fight.talent.TalentDataPoint;

public class GetPoolRunEloProgressionHandler implements HttpHandler {
    private final PoolFightDuelService poolFightDuelService;
    private final PoolRunDuelService poolRunDuelService;

    public GetPoolRunEloProgressionHandler(PoolFightDuelService poolFightDuelService, PoolRunDuelService poolRunDuelService) {
        this.poolFightDuelService = poolFightDuelService;
        this.poolRunDuelService = poolRunDuelService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            RequestUtils.sendResponse(exchange, 405, "Method not allowed");
            return;
        }

        try {
            String query = exchange.getRequestURI().getQuery();
            String poolRunId = RequestUtils.getQueryParam(query, "poolRunId");

            if (poolRunId == null || poolRunId.isEmpty()) {
                RequestUtils.sendResponse(exchange, 400, "Missing required field: poolRunId");
                return;
            }

            PoolRunDuel poolRunDuel = poolRunDuelService.getPoolRunDuelById(poolRunId);

            List<TalentDataPoint> talentDataPoints = poolFightDuelService.getEloProgressionByPoolRunId(poolRunId, poolRunDuel.leeks);

            JSONObject response = new JSONObject();
            response.put("eloProgression", talentDataPoints);
            response.put("success", true);

            RequestUtils.sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            System.err.println("Error in GetDuelFightsByPoolRunIdHandler: " + e.getMessage());
            e.printStackTrace();
            RequestUtils.sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

}
