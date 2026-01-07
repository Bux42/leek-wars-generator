package com.leekwars.api.mongo.repositories;

import java.util.Optional;

import org.bson.Document;

import com.leekwars.api.mongo.config.MongoClientProvider;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

public class PoolDuelRepository {
    private final MongoCollection<Document> duelPools;

    public PoolDuelRepository(MongoClientProvider provider) {
        this.duelPools = provider.getDatabase().getCollection("duel_pools");
    }

    public Optional<Document> findById(String id) {
        Document doc = duelPools.find(new Document("_id", new org.bson.types.ObjectId(id))).first();
        return Optional.ofNullable(doc);
    }

    public void insert(Document poolDuel) {
        duelPools.insertOne(poolDuel);
    }

    public Iterable<Document> findAll() {
        return duelPools.find();
    }

    public boolean update(Document poolDuel, String id) {
        UpdateResult result = duelPools.replaceOne(new Document("_id", new org.bson.types.ObjectId(id)), poolDuel);
        return result.getModifiedCount() > 0;
    }

    public boolean delete(String id) {
        DeleteResult result = duelPools.deleteOne(new Document("_id", new org.bson.types.ObjectId(id)));
        return result.getDeletedCount() > 0;
    }

    public boolean addLeekIdToPoolDuel(String poolDuelId, String leekId) {
        UpdateResult result = duelPools.updateOne(
            new Document("_id", new org.bson.types.ObjectId(poolDuelId)),
            new Document("$addToSet", new Document("leekIds", leekId))
        );
        return result.getMatchedCount() > 0;
    }

    public boolean removeLeekIdFromPoolDuel(String poolDuelId, String leekId) {
        UpdateResult result = duelPools.updateOne(
            new Document("_id", new org.bson.types.ObjectId(poolDuelId)),
            new Document("$pull", new Document("leekIds", leekId))
        );
        return result.getMatchedCount() > 0;
    }
}
