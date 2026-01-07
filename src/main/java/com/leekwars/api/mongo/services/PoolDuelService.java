package com.leekwars.api.mongo.services;

import java.util.List;

import com.leekwars.api.mongo.repositories.PoolDuelRepository;
import com.leekwars.pool.categories.PoolDuel;

public class PoolDuelService {
    private final PoolDuelRepository duelPools;

    public PoolDuelService(PoolDuelRepository duelPools) {
        this.duelPools = duelPools;
    }

    public List<PoolDuel> getAllPoolDuels() {
        List<PoolDuel> allPoolDuels =  new java.util.ArrayList<>();

        for (var doc : this.duelPools.findAll()) {
            // Convert MongoDB document to PoolDuel instance
            String docJson = doc.toJson();
            var poolDuelJson = com.alibaba.fastjson.JSON.parseObject(docJson);

            PoolDuel poolDuel = PoolDuel.fromJson(poolDuelJson);

            allPoolDuels.add(poolDuel);
        }

        return allPoolDuels;
    }

    public PoolDuel getPoolDuelById(String id) {
        var docOpt = this.duelPools.findById(id);
        if (docOpt.isEmpty()) {
            return null;
        }

        var doc = docOpt.get();
        // Convert MongoDB document to PoolDuel instance
        String docJson = doc.toJson();
        var poolDuelJson = com.alibaba.fastjson.JSON.parseObject(docJson);

        return PoolDuel.fromJson(poolDuelJson);
    }

    public String addPoolDuel(PoolDuel poolDuel) {
        // Convert PoolDuel instance to MongoDB Document
        String poolDuelJson = com.alibaba.fastjson.JSON.toJSONString(poolDuel);
        var poolDuelData = org.bson.Document.parse(poolDuelJson);

        // remove id field to avoid conflicts with MongoDB _id
        poolDuelData.remove("id");

        // Insert pool duel into database
        this.duelPools.insert(poolDuelData);

        // Return the ID of the newly created pool duel
        return poolDuelData.getObjectId("_id").toHexString();
    }

    public boolean updatePoolDuel(String id, PoolDuel poolDuel) {
        // Convert PoolDuel instance to MongoDB Document
        String poolDuelJson = com.alibaba.fastjson.JSON.toJSONString(poolDuel);
        var poolDuelData = org.bson.Document.parse(poolDuelJson);

        // remove id field to avoid conflicts with MongoDB _id
        poolDuelData.remove("id");

        // Update pool duel in database
        return this.duelPools.update(poolDuelData, id);
    }

    public boolean deletePoolDuel(String id) {
        return this.duelPools.delete(id);
    }

    public boolean addLeekIdToPoolDuel(String poolDuelId, String leekId) {
        return this.duelPools.addLeekIdToPoolDuel(poolDuelId, leekId);
    }

    public boolean removeLeekIdFromPoolDuel(String poolDuelId, String leekId) {
        return this.duelPools.removeLeekIdFromPoolDuel(poolDuelId, leekId);
    }
}
