package com.leekwars.api.mongo.repositories;

import java.util.Optional;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.leekwars.api.mongo.config.MongoClientProvider;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;

public class LeekRepository {
    private final MongoCollection<Document> leeks;

    public LeekRepository(MongoClientProvider provider) {
        this.leeks = provider.getDatabase().getCollection("leeks");
    }

    public Iterable<Document> findAll() {
        return leeks.find();
    }

    public Optional<Document> findById(String id) {
        Document doc = leeks.find(new Document("_id", new ObjectId(id))).first();
        return Optional.ofNullable(doc);
    }

    public void insert(Document leek) {
        leeks.insertOne(leek);
    }

    public void update(Document leek) {
        leeks.replaceOne(new Document("_id", leek.getString("_id")), leek);
    }

    public boolean delete(String id) {
        DeleteResult result = leeks.deleteOne(new Document("_id", new ObjectId(id)));
        return result.getDeletedCount() > 0;
    }
}
