package com.leekwars.api.mongo.repositories;

import java.util.Optional;

import org.bson.Document;

import com.leekwars.api.mongo.config.MongoClientProvider;
import com.mongodb.client.MongoCollection;

public class LeekRepository {
    private final MongoCollection<Document> leeks;

    public LeekRepository(MongoClientProvider provider) {
        this.leeks = provider.getDatabase().getCollection("leeks");
    }

    public Optional<Document> findById(String id) {
        Document doc = leeks.find(new Document("_id", id)).first();
        return Optional.ofNullable(doc);
    }

    public void insert(Document leek) {
        leeks.insertOne(leek);
    }

    public void update(Document leek) {
        leeks.replaceOne(new Document("_id", leek.getString("_id")), leek);
    }

    public void delete(String id) {
        leeks.deleteOne(new Document("_id", id));
    }
}
