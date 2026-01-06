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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.pool.categories.PoolDuel;
import com.leekwars.pool.leek.Leek;
import com.leekwars.pool.leek.LeekSnapshotAI;
import com.leekwars.pool.run.categories.PoolRunDuel;
import com.leekwars.pool.run.fight.PoolFightBase;

public class MongoDbManager {
    private MongoClient mongoClient;
    private MongoDatabase database;
    private String connectionString;
    private boolean isConnected = false;

    /**
     * Constructor with connection string
     * 
     * @param connectionString
     *            MongoDB connection string (e.g., "mongodb://localhost:27017")
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
     * 
     * @param databaseName
     *            Name of the database to connect to
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
     * 
     * @param collectionName
     *            Name of the collection
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
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Get the database instance
     * 
     * @return MongoDatabase or null if not connected
     */
    public MongoDatabase getDatabase() {
        return database;
    }

    /**
     * Get the connection string
     * 
     * @return connection string
     */
    public String getConnectionString() {
        return connectionString;
    }

    /**
     * Add a new leek to the leeks collection
     * 
     * @param leekData
     *            Document containing leek data (without id)
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
     * 
     * @param leekId
     *            The unique ID of the leek to update
     * @param updates
     *            Document containing the fields to update
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
     * 
     * @param leekId
     *            The unique ID of the leek to delete
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
     * Add a new Duel pool to the pools collection
     * 
     * @param poolData
     *            Document containing pool data (without id)
     * @return The generated unique ID for the pool, or null if failed
     */
    public String addPoolDuel(Document poolData) {
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
            MongoCollection<Document> poolsCollection = database.getCollection("pools_duel");
            poolsCollection.insertOne(poolData);
            return poolData.getString("id");
        } catch (Exception e) {
            System.err.println("Failed to add Duel pool: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get all duel pools as an array of PoolDuel objects
     * 
     * @return List of PoolDuel objects, or empty list if none found
     */
    public List<PoolDuel> getAllPoolDuels() {
        List<PoolDuel> pools = new ArrayList<>();
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return pools;
        }

        try {
            MongoCollection<Document> poolsCollection = database.getCollection("pools_duel");
            for (Document doc : poolsCollection.find()) {
                // Convert Document to PoolDuel object
                String docJson = doc.toJson();
                JSONObject jsonObject = JSON.parseObject(docJson);
                jsonObject.remove("_id"); // Remove MongoDB's _id field
                PoolDuel pool = PoolDuel.fromJson(jsonObject);
                pools.add(pool);
            }
        } catch (Exception e) {
            System.err.println("Failed to get all Duel pools: " + e.getMessage());
        }

        return pools;
    }

    /**
     * Get a Duel pool by its ID
     * 
     * @return Document containing pool data, or null if not found
     */

    public Document getPoolDuelById(String poolId) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return null;
        }

        try {
            MongoCollection<Document> poolsCollection = database.getCollection("pools_duel");
            Bson filter = Filters.eq("id", poolId);
            return poolsCollection.find(filter).first();
        } catch (Exception e) {
            System.err.println("Failed to get Duel pool by ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get all Duel pools from the pools collection
     * 
     * @return MongoCollection cursor for iteration, or null if failed
     */
    public MongoCollection<Document> getPoolDuelCollection() {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return null;
        }

        return database.getCollection("pools_duel");
    }

    /**
     * Update a duel pool document by its ID
     * 
     * @param poolId
     *            The unique ID of the pool to update
     * @param updates
     *            Document containing the fields to update
     * @return true if update was successful, false otherwise
     */
    public boolean updatePoolDuel(String poolId, Document updates) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return false;
        }

        try {
            MongoCollection<Document> poolsCollection = database.getCollection("pools_duel");

            // Create filter to find the pool by ID
            Bson filter = Filters.eq("id", poolId);

            // Create update document
            Bson updateOperation = new Document("$set", updates);

            // Perform the update
            UpdateResult result = poolsCollection.updateOne(filter, updateOperation);

            if (result.getMatchedCount() > 0) {
                System.out.println("Successfully updated duel pool with ID: " + poolId);
                return true;
            } else {
                System.err.println("No duel pool found with ID: " + poolId);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Failed to update duel pool: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a duel pool document by its ID
     * 
     * @param poolId
     *            The unique ID of the pool to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deletePoolDuel(String poolId) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return false;
        }

        try {
            MongoCollection<Document> poolsCollection = database.getCollection("pools_duel");

            // Create filter to find the pool by ID
            Bson filter = Filters.eq("id", poolId);

            // Perform the deletion
            DeleteResult result = poolsCollection.deleteOne(filter);

            if (result.getDeletedCount() > 0) {
                System.out.println("Successfully deleted duel pool with ID: " + poolId);
                return true;
            } else {
                System.err.println("No duel pool found with ID: " + poolId);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Failed to delete duel pool: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Clear stats for a 1v1 pool (reset total fights to 0)
     * 
     * @param poolId
     *            The unique ID of the pool to clear stats for
     * @return true if stats were cleared successfully, false otherwise
     */
    public boolean clearPool1v1Stats(String poolId) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return false;
        }

        try {
            MongoCollection<Document> poolsCollection = database.getCollection("pools_duel");

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
     * 
     * @param poolId
     *            The unique ID of the pool
     * @param enabled
     *            The enabled status to set
     * @return true if status was set successfully, false otherwise
     */
    public boolean setPool1v1Enabled(String poolId, boolean enabled) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return false;
        }

        try {
            MongoCollection<Document> poolsCollection = database.getCollection("pools_duel");

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
     * Get a list of Leek objects by their IDs
     * 
     * @param leekIds
     *            List of leek IDs
     * @return List of Leeks or empty list if none found
     */
    public List<Leek> getLeeksByIds(List<String> leekIds) {
        List<Leek> leeks = new ArrayList<>();
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return leeks;
        }

        try {
            MongoCollection<Document> leeksCollection = database.getCollection("leeks");
            Bson filter = Filters.in("id", leekIds);
            for (Document doc : leeksCollection.find(filter)) {
                // Convert Document to Leek object
                String docJson = doc.toJson();
                JSONObject jsonObject = JSON.parseObject(docJson);
                jsonObject.remove("_id"); // Remove MongoDB's _id field
                Leek leek = Leek.fromJson(jsonObject);
                leeks.add(leek);
            }
        } catch (Exception e) {
            System.err.println("Failed to get leeks by IDs: " + e.getMessage());
        }

        return leeks;
    }

    /**
     * Get a leek document by its ID
     * 
     * @param leekId
     *            The unique ID of the leek
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
     * Get a 1v1 pool by its ID
     * 
     * @param poolId
     *            The unique ID of the pool
     * @return PoolOneVersusOne object, or null if not found
     */
    public PoolDuel getPoolDuel(String poolId) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return null;
        }

        try {
            MongoCollection<Document> poolsCollection = database.getCollection("pools_duel");
            Bson filter = Filters.eq("id", poolId);
            Document doc = poolsCollection.find(filter).first();

            if (doc != null) {
                // Convert Document to JSON and then to PoolDuel
                String docJson = doc.toJson();
                JSONObject jsonObject = JSON.parseObject(docJson);
                jsonObject.remove("_id"); // Remove MongoDB's _id field
                return PoolDuel.fromJson(jsonObject);
            }

            return null;
        } catch (Exception e) {
            System.err.println("Failed to get 1v1 pool by ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Add a leek ID to a duel pool's leek_ids list
     * 
     * @param poolId
     *            The unique ID of the pool
     * @param leekId
     *            The leek ID to add to the pool
     * @return true if leek was added successfully, false otherwise
     */
    public boolean addLeekToPoolDuel(String poolId, String leekId) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return false;
        }

        try {
            MongoCollection<Document> poolsCollection = database.getCollection("pools_duel");

            // Create filter to find the pool by ID
            Bson filter = Filters.eq("id", poolId);

            // Create update to add leek ID to the array
            Bson updateOperation = Updates.addToSet("leek_ids", leekId);

            // Perform the update
            UpdateResult result = poolsCollection.updateOne(filter, updateOperation);

            if (result.getMatchedCount() > 0) {
                System.out.println("Successfully added leek " + leekId + " to duel pool with ID: " + poolId);
                return true;
            } else {
                System.err.println("No duel pool found with ID: " + poolId);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Failed to add leek to duel pool: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Enable fight count limit for a 1v1 pool and set the limit value
     * 
     * @param poolId
     *            The unique ID of the pool
     * @param limit
     *            The fight count limit to set
     * @return true if limit was enabled successfully, false otherwise
     */
    public boolean enablePool1v1FightCountLimit(String poolId, int limit) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return false;
        }

        try {
            MongoCollection<Document> poolsCollection = database.getCollection("pools_duel");

            // Create filter to find the pool by ID
            Bson filter = Filters.eq("id", poolId);

            // Create update to enable fight count limit and set the limit
            Document updates = new Document()
                    .append("fight_count_limit_enabled", true)
                    .append("fight_count_limit", limit);

            Bson updateOperation = new Document("$set", updates);

            // Perform the update
            UpdateResult result = poolsCollection.updateOne(filter, updateOperation);

            if (result.getMatchedCount() > 0) {
                System.out.println("Successfully enabled fight count limit (" + limit + ") for 1v1 pool with ID: " + poolId);
                return true;
            } else {
                System.err.println("No 1v1 pool found with ID: " + poolId);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Failed to enable fight count limit for 1v1 pool: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Disable fight count limit for a 1v1 pool
     * 
     * @param poolId
     *            The unique ID of the pool
     * @return true if limit was disabled successfully, false otherwise
     */
    public boolean disablePool1v1FightCountLimit(String poolId) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return false;
        }

        try {
            MongoCollection<Document> poolsCollection = database.getCollection("pools_duel");

            // Create filter to find the pool by ID
            Bson filter = Filters.eq("id", poolId);

            // Create update to disable fight count limit
            Document updates = new Document("fight_count_limit_enabled", false);
            Bson updateOperation = new Document("$set", updates);

            // Perform the update
            UpdateResult result = poolsCollection.updateOne(filter, updateOperation);

            if (result.getMatchedCount() > 0) {
                System.out.println("Successfully disabled fight count limit for 1v1 pool with ID: " + poolId);
                return true;
            } else {
                System.err.println("No 1v1 pool found with ID: " + poolId);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Failed to disable fight count limit for 1v1 pool: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Reset elo for all leeks in a 1v1 pool
     * 
     * @param poolId
     *            The unique ID of the pool
     * @param elo
     *            The elo value to reset all leeks to
     * @return true if all leeks' elo were reset successfully, false otherwise
     */
    public boolean resetPool1v1LeeksElo(String poolId, int elo) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return false;
        }

        try {
            // Get the pool to access its leek_ids
            PoolDuel pool = getPoolDuel(poolId);
            if (pool == null) {
                System.err.println("No duel pool found with ID: " + poolId);
                return false;
            }

            MongoCollection<Document> leeksCollection = database.getCollection("leeks");
            int updatedCount = 0;

            // Update elo for each leek in the pool
            for (String leekId : pool.leek_ids) {
                Bson filter = Filters.eq("id", leekId);
                Document updates = new Document("elo", elo);
                Bson updateOperation = new Document("$set", updates);

                UpdateResult result = leeksCollection.updateOne(filter, updateOperation);
                if (result.getMatchedCount() > 0) {
                    updatedCount++;
                } else {
                    System.err.println("Warning: Leek with ID " + leekId + " not found");
                }
            }

            System.out.println("Successfully reset elo to " + elo + " for " + updatedCount + " leeks in pool " + poolId);
            return updatedCount > 0;
        } catch (Exception e) {
            System.err.println("Failed to reset leeks' elo for pool: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update a poolRun duel item in the pool_runs_duel collection
     * @param poolRunDuel
     * @return true if update was successful, false otherwise
     */
    public boolean updatePoolRunDuel(PoolRunDuel poolRunDuel) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return false;
        }

        try {
            MongoCollection<Document> poolRunsCollection = database.getCollection("pool_runs_duel");

            // Create filter to find the pool run by ID
            Bson filter = Filters.eq("id", poolRunDuel.id);

            // Convert PoolRunDuel to Document
            String jsonString = JSON.toJSONString(poolRunDuel);
            JSONObject jsonObject = JSON.parseObject(jsonString);
            Document doc = Document.parse(jsonObject.toJSONString());

            // Create update document
            Bson updateOperation = new Document("$set", doc);

            // Perform the update
            UpdateResult result = poolRunsCollection.updateOne(filter, updateOperation);

            if (result.getMatchedCount() > 0) {
                System.out.println("Successfully updated PoolRunDuel with ID: " + poolRunDuel.id);
                return true;
            } else {
                System.err.println("No PoolRunDuel found with ID: " + poolRunDuel.id);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Failed to update PoolRunDuel: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create a new poolRun duel item in the pool_runs_duel collection
     * 
     * @param PoolRunDuel
     *            poolRunDuel The PoolRunDuel object to store
     * @return The ID of the created PoolRunDuel if successful, null otherwise
     */
    public String createPoolRunDuel(PoolRunDuel poolRunDuel) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return null;
        }

        try {
            MongoCollection<Document> poolRunsCollection = database.getCollection("pool_runs_duel");

            // Generate a unique ID
            String uniqueId = UUID.randomUUID().toString();
            poolRunDuel.id = uniqueId;

            // Convert PoolRunDuel to Document
            String jsonString = JSON.toJSONString(poolRunDuel);
            JSONObject jsonObject = JSON.parseObject(jsonString);
            Document doc = Document.parse(jsonObject.toJSONString());

            // Insert into the pool_runs_duel collection
            poolRunsCollection.insertOne(doc);

            System.out.println("Successfully created PoolRunDuel with ID: " + poolRunDuel.id);
            return poolRunDuel.id;
        } catch (Exception e) {
            System.err.println("Failed to create PoolRunDuel: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Add a fight duo item to  in the pool_fights_duel collection
     * 
     * @param fightData The PoolFightDuo object to add
     */
    public void addFightItem(Object fightData) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return;
        }

        try {
            MongoCollection<Document> poolFightsCollection = database.getCollection("pool_fights");

            // Generate a unique ID
            String uniqueId = UUID.randomUUID().toString();
            ((PoolFightBase) fightData).id = uniqueId;

            // Convert PoolFightDuo to Document
            String jsonString = JSON.toJSONString(fightData);
            JSONObject jsonObject = JSON.parseObject(jsonString);
            Document doc = Document.parse(jsonObject.toJSONString());

            // Insert into the pool_fights_duel collection
            poolFightsCollection.insertOne(doc);

            // System.out.println("Successfully added fight to PoolRunDuel with ID: " + fightData.id);
        } catch (Exception e) {
            System.err.println("Failed to add fight to PoolRunDuel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update the elo of leeks within a poolRun duel item in the pool_runs_duel collection
     * First, get the PoolRunDuel by its ID, then find the leeks within the leeks array and update their elo values.
     * 
     * @param poolRunDuelId The ID of the PoolRunDuel to update
     * @param leek1 The first Leek object
     * @param leek1Elo The new elo for the first leek
     * @param leek2 The second Leek object
     * @param leek2Elo The new elo for the second leek
     */
    public void updateFightDuoLeeksElo(String poolRunDuelId, Leek leek1, int leek1Elo, Leek leek2, int leek2Elo) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return;
        }

        try {
            MongoCollection<Document> poolRunsCollection = database.getCollection("pool_runs_duel");
            // Create filter to find the pool run by ID
            Bson filter = Filters.eq("id", poolRunDuelId);
            Document poolRunDoc = poolRunsCollection.find(filter).first();
            if (poolRunDoc == null) {
                System.err.println("No PoolRunDuel found with ID: " + poolRunDuelId);
                return;
            }
            List<Document> leeks = (List<Document>) poolRunDoc.get("leeks");
            for (Document leekDoc : leeks) {
                String leekId = leekDoc.getString("id");
                if (leekId.equals(leek1.id)) {
                    leekDoc.put("elo", leek1Elo);
                } else if (leekId.equals(leek2.id)) {
                    leekDoc.put("elo", leek2Elo);
                }
            }
            // Update the leeks array in the document
            Bson updateOperation = new Document("$set", new Document("leeks", leeks));
            UpdateResult result = poolRunsCollection.updateOne(filter, updateOperation);
            if (result.getMatchedCount() > 0) {
                // System.out.println("Successfully updated leeks' elo in PoolRunDuel with ID: " + poolRunDuelId);
            } else {
                System.err.println("No PoolRunDuel found with ID: " + poolRunDuelId);
            }

        } catch (Exception e) {
            System.err.println("Failed to update leeks' elo: " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Adds a LeekSnapshotAI document to the leek_snapshots_ai collection
     * @param snapshot The LeekSnapshotAI object to add
     * @return The ID of the created snapshot if successful, null otherwise
     */
    public String addLeekSnapshotAI(LeekSnapshotAI snapshot) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return null;
        }

        try {
            MongoCollection<Document> snapshotsCollection = database.getCollection("leek_snapshots_ai");

            // Convert LeekSnapshotAI to Document
            String jsonString = JSON.toJSONString(snapshot);
            JSONObject jsonObject = JSON.parseObject(jsonString);
            Document doc = Document.parse(jsonObject.toJSONString());

            // Insert into the leek_snapshots_ai collection
            snapshotsCollection.insertOne(doc);

            System.out.println("Successfully added LeekSnapshotAI with mergedAiCodeHash: " + snapshot.mergedAiCodeHash);
            return snapshot.mergedAiCodeHash;
        } catch (Exception e) {
            System.err.println("Failed to add LeekSnapshotAI: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets a LeekSnapshotAI document from the leek_snapshots_ai collection
     * @param mergedAiCodeHash The merged AI code hash to search for
     * @return The LeekSnapshotAI object if found, null otherwise
     */
    public LeekSnapshotAI getLeekSnapshotAI(String mergedAiCodeHash) {
        if (!isConnected || database == null) {
            System.err.println("Not connected to MongoDB database");
            return null;
        }

        try {
            MongoCollection<Document> snapshotsCollection = database.getCollection("leek_snapshots_ai");
            Bson filter = Filters.eq("mergedAiCodeHash", mergedAiCodeHash);
            Document doc = snapshotsCollection.find(filter).first();

            if (doc != null) {
                // Convert Document to LeekSnapshotAI object
                String docJson = doc.toJson();
                JSONObject jsonObject = JSON.parseObject(docJson);
                jsonObject.remove("_id"); // Remove MongoDB's _id field
                return LeekSnapshotAI.fromJson(jsonObject);
            }

            return null;
        } catch (Exception e) {
            System.err.println("Failed to get LeekSnapshotAI by mergedAiCodeHash: " + e.getMessage());
            return null;
        }
    }
}