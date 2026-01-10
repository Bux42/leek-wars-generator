package com.leekwars.api.mongo.services;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.repositories.PoolFightDuelRepository;
import com.leekwars.pool.run.fight.categories.PoolFightDuel;

public class PoolFightDuelService {
    private final PoolFightDuelRepository poolFightDuel;

    public PoolFightDuelService(PoolFightDuelRepository poolFightDuel) {
        this.poolFightDuel = poolFightDuel;
    }

    public String addPoolFight(PoolFightDuel poolFight) {
        // Convert PoolDuel instance to MongoDB Document
        String fightJson = com.alibaba.fastjson.JSON.toJSONString(poolFight);
        var fightData = org.bson.Document.parse(fightJson);

        // remove id field to avoid conflicts with MongoDB _id
        fightData.remove("id");

        // Insert pool duel into database
        this.poolFightDuel.insert(fightData);
        return fightData.getObjectId("_id").toHexString();
    }

    public int countAllPoolFights() {
        return this.poolFightDuel.countAll();
    }

    public int countAllPoolFightsByPoolRunId(String poolRunId) {
        return this.poolFightDuel.countAllByPoolRunId(poolRunId);
    }

    public List<PoolFightDuel> getAllByPoolRunId(String poolRunId) {
        var docs = this.poolFightDuel.getAllByPoolRunId(poolRunId);
        if (docs == null) {
            return null;
        }

        List<PoolFightDuel> poolFightsDuel = new ArrayList<>();

        for (var doc : docs) {
            String duelJson = doc.toJson();
            poolFightsDuel.add(PoolFightDuel.fromJson(JSON.parseObject(duelJson, JSONObject.class)));
        }

        return poolFightsDuel;
    }
}
