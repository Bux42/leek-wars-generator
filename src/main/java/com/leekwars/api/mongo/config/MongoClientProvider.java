package com.leekwars.api.mongo.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoClientProvider {
    private final MongoClient client;
    private final MongoDatabase db;
    private boolean connected = true;

    public MongoClientProvider(String uri, String dbName) {
        try {
            this.client = MongoClients.create(uri);
            this.db = client.getDatabase(dbName);
            connected = true;
        } catch (Exception e) {
            connected = false;
            throw new RuntimeException("Failed to connect to MongoDB: " + e.getMessage(), e);
        }
    }

    public MongoDatabase getDatabase() {
        return db;
    }

    public boolean isConnected() {
        return connected;
    }
}
