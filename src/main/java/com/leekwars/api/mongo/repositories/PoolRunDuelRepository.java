package com.leekwars.api.mongo.repositories;

import java.util.Optional;

import org.bson.Document;

import com.leekwars.api.mongo.config.MongoClientProvider;
import com.mongodb.client.MongoCollection;

public class PoolRunDuelRepository {
    private final MongoCollection<Document> duelPoolRuns;

    public PoolRunDuelRepository(MongoClientProvider provider) {
        this.duelPoolRuns = provider.getDatabase().getCollection("pool_run_duels");
    }

    public Optional<Document> findById(String id) {
        Document doc = duelPoolRuns.find(new Document("_id", new org.bson.types.ObjectId(id))).first();
        return Optional.ofNullable(doc);
    }

    public String insert(Document poolDuel) {
        duelPoolRuns.insertOne(poolDuel);
        return poolDuel.getObjectId("_id").toHexString();
    }

    public Iterable<Document> findAll() {
        return duelPoolRuns.find();
    }

    public boolean update(Document poolDuel, String id) {
        var result = duelPoolRuns.replaceOne(new Document("_id", new org.bson.types.ObjectId(id)), poolDuel);
        return result.getModifiedCount() > 0;
    }

    public boolean updateLeekEloById(String poolDuelId, String leekId, int newElo) {
        // find leek object in leeks array and update its elo
        var result = duelPoolRuns.updateOne(
            new Document("_id", new org.bson.types.ObjectId(poolDuelId))
                .append("leeks.id", leekId),
            new Document("$set", new Document("leeks.$.elo", newElo))
        );
        return result.getMatchedCount() > 0;
    }
}
