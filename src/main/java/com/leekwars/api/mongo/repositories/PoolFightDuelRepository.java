package com.leekwars.api.mongo.repositories;

import org.bson.Document;

import com.leekwars.api.mongo.config.MongoClientProvider;
import com.mongodb.client.MongoCollection;

public class PoolFightDuelRepository {
    private final MongoCollection<Document> poolFights;

    public PoolFightDuelRepository(MongoClientProvider provider) {
        this.poolFights = provider.getDatabase().getCollection("pool_fight_duel");
    }

    public String insert(Document poolFight) {
        poolFights.insertOne(poolFight);
        return poolFight.getObjectId("_id").toHexString();
    }
}
