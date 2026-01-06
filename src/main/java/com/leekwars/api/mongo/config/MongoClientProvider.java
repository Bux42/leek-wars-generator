package com.leekwars.api.mongo.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoClientProvider {
    private final MongoClient client;
    private final MongoDatabase db;

    public MongoClientProvider(String uri, String dbName) {
        this.client = MongoClients.create(uri);
        this.db = client.getDatabase(dbName);
    }

    public MongoDatabase getDatabase() {
        return db;
    }
}
