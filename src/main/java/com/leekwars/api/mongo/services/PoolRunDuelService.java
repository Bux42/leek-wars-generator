package com.leekwars.api.mongo.services;

import org.bson.Document;

import com.leekwars.api.mongo.repositories.PoolRunDuelRepository;
import com.leekwars.pool.run.categories.PoolRunDuel;

public class PoolRunDuelService {
    private final PoolRunDuelRepository duelPoolRuns;

    public PoolRunDuelService(PoolRunDuelRepository duelPoolRuns) {
        this.duelPoolRuns = duelPoolRuns;
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
