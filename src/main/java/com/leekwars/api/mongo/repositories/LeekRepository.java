package com.leekwars.api.mongo.repositories;

import java.util.Optional;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.leekwars.api.mongo.config.MongoClientProvider;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

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

    public boolean update(Document leek, String id) {
        UpdateResult result = leeks.replaceOne(new Document("_id", new ObjectId(id)), leek);
        return result.getModifiedCount() > 0;
    }

    public boolean delete(String id) {
        DeleteResult result = leeks.deleteOne(new Document("_id", new ObjectId(id)));
        return result.getDeletedCount() > 0;
    }
}
