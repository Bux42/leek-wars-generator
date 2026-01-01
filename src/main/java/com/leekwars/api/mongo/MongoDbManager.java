package com.leekwars.api.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.util.UUID;

public class MongoDbManager {
    private MongoClient mongoClient;
    private MongoDatabase database;
    private String connectionString;
    private boolean isConnected = false;

    /**
     * Constructor with connection string
     * @param connectionString MongoDB connection string (e.g., "mongodb://localhost:27017")
     */
    public MongoDbManager(String connectionString) {
        this.connectionString = connectionString;
    }

    /**
     * Default constructor using localhost
     */
    public MongoDbManager() {
        this("mongodb://localhost:27017");
    }

    /**
     * Connect to MongoDB database
     * @param databaseName Name of the database to connect to
     * @return true if connection successful, false otherwise
     */
    public boolean connect(String databaseName) {
        try {
            System.out.println("Connecting to MongoDB at: " + connectionString);
            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase(databaseName);
            
            // Test the connection by running a ping command
            database.runCommand(new Document("ping", 1));
            
            isConnected = true;
            System.out.println("Successfully connected to MongoDB database: " + databaseName);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to connect to MongoDB: " + e.getMessage());
            e.printStackTrace();
            isConnected = false;
            return false;
        }
    }

    /**
     * Get a collection from the database
     * @param collectionName Name of the collection
     * @return MongoCollection or null if not connected
     */
    public MongoCollection<Document> getCollection(String collectionName) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return null;
        }
        return database.getCollection(collectionName);
    }

    /**
     * Close the MongoDB connection
     */
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            isConnected = false;
            System.out.println("MongoDB connection closed");
        }
    }

    /**
     * Check if connected to MongoDB
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Get the database instance
     * @return MongoDatabase or null if not connected
     */
    public MongoDatabase getDatabase() {
        return database;
    }

    /**
     * Get the connection string
     * @return connection string
     */
    public String getConnectionString() {
        return connectionString;
    }
    
    /**
     * Add a new leek to the leeks collection
     * @param leekData Document containing leek data (without id)
     * @return The generated unique ID for the leek, or null if failed
     */
    public String addLeek(Document leekData) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return null;
        }
        
        try {
            // Generate a unique ID
            String uniqueId = UUID.randomUUID().toString();
            
            // Add the ID to the document
            leekData.append("id", uniqueId);
            
            // Insert into the leeks collection
            MongoCollection<Document> leeksCollection = database.getCollection("leeks");
            leeksCollection.insertOne(leekData);
            
            System.out.println("Successfully added leek with ID: " + uniqueId);
            return uniqueId;
        } catch (Exception e) {
            System.err.println("Failed to add leek: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Update a leek document by its ID
     * @param leekId The unique ID of the leek to update
     * @param updates Document containing the fields to update
     * @return true if update was successful, false otherwise
     */
    public boolean updateLeek(String leekId, Document updates) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return false;
        }
        
        try {
            MongoCollection<Document> leeksCollection = database.getCollection("leeks");
            
            // Create filter to find the leek by ID
            Bson filter = Filters.eq("id", leekId);
            
            // Create update document
            Bson updateOperation = new Document("$set", updates);
            
            // Perform the update
            UpdateResult result = leeksCollection.updateOne(filter, updateOperation);
            
            if (result.getMatchedCount() > 0) {
                System.out.println("Successfully updated leek with ID: " + leekId);
                return true;
            } else {
                System.err.println("No leek found with ID: " + leekId);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Failed to update leek: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Delete a leek document by its ID
     * @param leekId The unique ID of the leek to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteLeek(String leekId) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return false;
        }
        
        try {
            MongoCollection<Document> leeksCollection = database.getCollection("leeks");
            
            // Create filter to find the leek by ID
            Bson filter = Filters.eq("id", leekId);
            
            // Perform the deletion
            DeleteResult result = leeksCollection.deleteOne(filter);
            
            if (result.getDeletedCount() > 0) {
                System.out.println("Successfully deleted leek with ID: " + leekId);
                return true;
            } else {
                System.err.println("No leek found with ID: " + leekId);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Failed to delete leek: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // ========== POOL ONE VS ONE METHODS ==========
    
    /**
     * Add a new 1v1 pool to the pools collection
     * @param poolData Document containing pool data (without id)
     * @return The generated unique ID for the pool, or null if failed
     */
    public String addPool1v1(Document poolData) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return null;
        }
        
        try {
            // Generate a unique ID if not present
            if (!poolData.containsKey("id")) {
                String uniqueId = UUID.randomUUID().toString();
                poolData.append("id", uniqueId);
            }
            
            // Insert into the pools collection
            MongoCollection<Document> poolsCollection = database.getCollection("pools_1v1");
            poolsCollection.insertOne(poolData);
            
            System.out.println("Successfully added 1v1 pool with ID: " + poolData.getString("id"));
            return poolData.getString("id");
        } catch (Exception e) {
            System.err.println("Failed to add 1v1 pool: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get all 1v1 pools from the pools collection
     * @return MongoCollection cursor for iteration, or null if failed
     */
    public MongoCollection<Document> getPool1v1Collection() {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return null;
        }
        
        return database.getCollection("pools_1v1");
    }
    
    /**
     * Update a 1v1 pool document by its ID
     * @param poolId The unique ID of the pool to update
     * @param updates Document containing the fields to update
     * @return true if update was successful, false otherwise
     */
    public boolean updatePool1v1(String poolId, Document updates) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return false;
        }
        
        try {
            MongoCollection<Document> poolsCollection = database.getCollection("pools_1v1");
            
            // Create filter to find the pool by ID
            Bson filter = Filters.eq("id", poolId);
            
            // Create update document
            Bson updateOperation = new Document("$set", updates);
            
            // Perform the update
            UpdateResult result = poolsCollection.updateOne(filter, updateOperation);
            
            if (result.getMatchedCount() > 0) {
                System.out.println("Successfully updated 1v1 pool with ID: " + poolId);
                return true;
            } else {
                System.err.println("No 1v1 pool found with ID: " + poolId);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Failed to update 1v1 pool: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Delete a 1v1 pool document by its ID
     * @param poolId The unique ID of the pool to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deletePool1v1(String poolId) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return false;
        }
        
        try {
            MongoCollection<Document> poolsCollection = database.getCollection("pools_1v1");
            
            // Create filter to find the pool by ID
            Bson filter = Filters.eq("id", poolId);
            
            // Perform the deletion
            DeleteResult result = poolsCollection.deleteOne(filter);
            
            if (result.getDeletedCount() > 0) {
                System.out.println("Successfully deleted 1v1 pool with ID: " + poolId);
                return true;
            } else {
                System.err.println("No 1v1 pool found with ID: " + poolId);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Failed to delete 1v1 pool: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Clear stats for a 1v1 pool (reset total fights to 0)
     * @param poolId The unique ID of the pool to clear stats for
     * @return true if stats were cleared successfully, false otherwise
     */
    public boolean clearPool1v1Stats(String poolId) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return false;
        }
        
        try {
            MongoCollection<Document> poolsCollection = database.getCollection("pools_1v1");
            
            // Create filter to find the pool by ID
            Bson filter = Filters.eq("id", poolId);
            
            // Create update to reset stats
            Document resetStats = new Document("total_fights", 0);
            
            Bson updateOperation = new Document("$set", resetStats);
            
            // Perform the update
            UpdateResult result = poolsCollection.updateOne(filter, updateOperation);
            
            if (result.getMatchedCount() > 0) {
                System.out.println("Successfully cleared stats for 1v1 pool with ID: " + poolId);
                return true;
            } else {
                System.err.println("No 1v1 pool found with ID: " + poolId);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Failed to clear 1v1 pool stats: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Set the enabled status for a 1v1 pool
     * @param poolId The unique ID of the pool
     * @param enabled The enabled status to set
     * @return true if status was set successfully, false otherwise
     */
    public boolean setPool1v1Enabled(String poolId, boolean enabled) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return false;
        }
        
        try {
            MongoCollection<Document> poolsCollection = database.getCollection("pools_1v1");
            
            // Create filter to find the pool by ID
            Bson filter = Filters.eq("id", poolId);
            
            // Create update to set enabled status
            Document enabledUpdate = new Document("enabled", enabled);
            Bson updateOperation = new Document("$set", enabledUpdate);
            
            // Perform the update
            UpdateResult result = poolsCollection.updateOne(filter, updateOperation);
            
            if (result.getMatchedCount() > 0) {
                System.out.println("Successfully set enabled=" + enabled + " for 1v1 pool with ID: " + poolId);
                return true;
            } else {
                System.err.println("No 1v1 pool found with ID: " + poolId);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Failed to set enabled status for 1v1 pool: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get a leek document by its ID
     * @param leekId The unique ID of the leek
     * @return Document containing leek data, or null if not found
     */
    public Document getLeekById(String leekId) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return null;
        }
        
        try {
            MongoCollection<Document> leeksCollection = database.getCollection("leeks");
            Bson filter = Filters.eq("id", leekId);
            return leeksCollection.find(filter).first();
        } catch (Exception e) {
            System.err.println("Failed to get leek by ID: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Add a leek ID to a 1v1 pool's leek_ids list
     * @param poolId The unique ID of the pool
     * @param leekId The leek ID to add to the pool
     * @return true if leek was added successfully, false otherwise
     */
    public boolean addLeekToPool1v1(String poolId, String leekId) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return false;
        }
        
        try {
            MongoCollection<Document> poolsCollection = database.getCollection("pools_1v1");
            
            // Create filter to find the pool by ID
            Bson filter = Filters.eq("id", poolId);
            
            // Create update to add leek ID to the array
            Bson updateOperation = Updates.addToSet("leek_ids", leekId);
            
            // Perform the update
            UpdateResult result = poolsCollection.updateOne(filter, updateOperation);
            
            if (result.getMatchedCount() > 0) {
                System.out.println("Successfully added leek " + leekId + " to 1v1 pool with ID: " + poolId);
                return true;
            } else {
                System.err.println("No 1v1 pool found with ID: " + poolId);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Failed to add leek to 1v1 pool: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
