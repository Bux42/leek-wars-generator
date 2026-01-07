package com.leekwars.api.mongo.services;

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
}
