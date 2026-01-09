package com.leekwars.api.mongo.services;

import java.util.ArrayList;
import java.util.List;

import com.leekwars.api.mongo.repositories.PoolRunDuelRepository;
import com.leekwars.pool.run.categories.PoolRunDuel;

public class PoolRunDuelService {
    private final PoolRunDuelRepository duelPoolRuns;

    public PoolRunDuelService(PoolRunDuelRepository duelPoolRuns) {
        this.duelPoolRuns = duelPoolRuns;
    }

    public List<PoolRunDuel> getAllPoolRunDuels() {
        var docs = this.duelPoolRuns.findAll();
        List<PoolRunDuel> poolRunDuels = new ArrayList<>();

        for (var doc : docs) {
            String duelJson = doc.toJson();
            PoolRunDuel poolRunDuel = PoolRunDuel.fromJson(com.alibaba.fastjson.JSON.parseObject(duelJson, com.alibaba.fastjson.JSONObject.class));
            poolRunDuels.add(poolRunDuel);
        }
        return poolRunDuels;
    }

    public PoolRunDuel getPoolRunDuelById(String id) {
        var docOpt = this.duelPoolRuns.findById(id);
        if (docOpt.isEmpty()) {
            return null;
        }
        String duelJson = docOpt.get().toJson();
        return PoolRunDuel.fromJson(com.alibaba.fastjson.JSON.parseObject(duelJson, com.alibaba.fastjson.JSONObject.class));
    }

    public List<PoolRunDuel> getAllPoolRunDuelByPoolId(String poolId) {
        var docs = this.duelPoolRuns.findAllByPoolid(poolId);
        if (docs == null) {
            return null;
        }
        List<PoolRunDuel> poolRunDuels = new ArrayList<>();
        for (var doc : docs) {
            String duelJson = doc.toJson();
            poolRunDuels.add(PoolRunDuel.fromJson(com.alibaba.fastjson.JSON.parseObject(duelJson, com.alibaba.fastjson.JSONObject.class)));
        }
        return poolRunDuels;
    }

    public String addPoolRunDuel(PoolRunDuel poolRunDuel) {
        // Convert PoolRunDuel instance to MongoDB Document
        String duelJson = com.alibaba.fastjson.JSON.toJSONString(poolRunDuel);
        var duelData = org.bson.Document.parse(duelJson);

        // Insert document into MongoDB
        this.duelPoolRuns.insert(duelData);

        // Return the generated ID
        return duelData.getObjectId("_id").toString();
    }

    public boolean updatePoolRunDuel(String id, PoolRunDuel poolRunDuel) {
        var docOpt = this.duelPoolRuns.findById(id);
        if (docOpt.isEmpty()) {
            return false;
        }

        // Convert PoolRunDuel instance to MongoDB Document
        String duelJson = com.alibaba.fastjson.JSON.toJSONString(poolRunDuel);
        var duelData = org.bson.Document.parse(duelJson);

        // remove id field to avoid conflict during update
        duelData.remove("id");
        
        return this.duelPoolRuns.update(duelData, id);
    }

    public boolean updateLeekEloById(String poolDuelId, String leekId, int newElo) {
        return this.duelPoolRuns.updateLeekEloById(poolDuelId, leekId, newElo);
    }
}
