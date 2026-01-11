package com.leekwars.api.mongo.repositories;

import java.util.Optional;

import org.bson.Document;

import com.leekwars.api.mongo.config.MongoClientProvider;
import com.mongodb.client.MongoCollection;

public class LeekscriptAiRepository {
    private final MongoCollection<Document> leekscriptAis;

    public LeekscriptAiRepository(MongoClientProvider provider) {
        this.leekscriptAis = provider.getDatabase().getCollection("leekscript_ais");
    }

    public Iterable<Document> findAll() {
        return leekscriptAis.find();
    }

    public Optional<Document> findById(String id) {
        Document doc = leekscriptAis.find(new Document("_id", id)).first();
        return Optional.ofNullable(doc);
    }

    public Optional<Document> findByMergedAiCodeHash(String mergedAiCodeHash) {
        // check document.mergedCode.hash == mergedAiCodeHash
        Document doc = leekscriptAis.find(new Document("mergedCode.hash", mergedAiCodeHash)).first();
        return Optional.ofNullable(doc);
    }

    public void insert(Document leekAi) {
        leekscriptAis.insertOne(leekAi);
    }

    public boolean update(Document leekAi, String id) {
        var result = leekscriptAis.replaceOne(new Document("_id", id), leekAi);
        return result.getModifiedCount() > 0;
    }

    public boolean delete(String id) {
        var result = leekscriptAis.deleteOne(new Document("_id", id));
        return result.getDeletedCount() > 0;
    }
}
