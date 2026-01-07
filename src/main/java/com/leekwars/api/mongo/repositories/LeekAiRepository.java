package com.leekwars.api.mongo.repositories;

import java.lang.classfile.ClassFile.Option;
import java.util.Optional;

import org.bson.Document;

import com.leekwars.api.mongo.config.MongoClientProvider;
import com.mongodb.client.MongoCollection;

public class LeekAiRepository {
    private final MongoCollection<Document> leekAis;

    public LeekAiRepository(MongoClientProvider provider) {
        this.leekAis = provider.getDatabase().getCollection("leekAis");
    }

    public Iterable<Document> findAll() {
        return leekAis.find();
    }

    public Optional<Document> findById(String id) {
        Document doc = leekAis.find(new Document("_id", id)).first();
        return Optional.ofNullable(doc);
    }

    public Optional<Document> findByMergedAiCodeHash(String mergedAiCodeHash) {
        // check document.mergedCode.hash == mergedAiCodeHash
        Document doc = leekAis.find(new Document("mergedCode.hash", mergedAiCodeHash)).first();
        return Optional.ofNullable(doc);
    }

    public void insert(Document leekAi) {
        leekAis.insertOne(leekAi);
    }

    public boolean update(Document leekAi, String id) {
        var result = leekAis.replaceOne(new Document("_id", id), leekAi);
        return result.getModifiedCount() > 0;
    }

    public boolean delete(String id) {
        var result = leekAis.deleteOne(new Document("_id", id));
        return result.getDeletedCount() > 0;
    }
}
