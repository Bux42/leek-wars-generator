package com.leekwars.pool;

import com.leekwars.pool.scenarios.ScenarioManager;
import com.leekwars.api.mongo.MongoDbManager;
import com.leekwars.api.mongo.scenarios.PoolOneVersusOne;
import java.util.concurrent.*;
import java.util.Map;
import java.util.HashMap;

public class PoolManager {
    private ScenarioManager scenarioManager = new ScenarioManager();
    private MongoDbManager mongoDbManager;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private Map<String, ScheduledFuture<?>> runningPools = new ConcurrentHashMap<>();
    
    public PoolManager(MongoDbManager mongoDbManager) {
        this.mongoDbManager = mongoDbManager;
        System.out.println("PoolManager: Init");
    }
    
    /**
     * Start a 1v1 pool - increments total_fights every second
     * @param poolId The ID of the pool to start
     * @return true if pool was started, false if already running or pool not found
     */
    public boolean startPool1v1(String poolId) {
        // Check if pool is already running
        if (runningPools.containsKey(poolId)) {
            System.out.println("Pool " + poolId + " is already running");
            return false;
        }
        
        // Verify pool exists in database
        PoolOneVersusOne pool = mongoDbManager.getPool1v1ById(poolId);
        if (pool == null) {
            System.err.println("Pool " + poolId + " not found in database");
            return false;
        }
        
        System.out.println("Starting pool: " + pool.name + " (ID: " + poolId + ")");
        
        // Schedule task to increment total_fights every second
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                // Get current pool data
                PoolOneVersusOne currentPool = mongoDbManager.getPool1v1ById(poolId);
                if (currentPool != null) {
                    int newTotal = currentPool.total_executed_fights + 1;
                    
                    // Update database
                    org.bson.Document updates = new org.bson.Document("total_fights", newTotal);
                    mongoDbManager.updatePool1v1(poolId, updates);
                    
                    System.out.println("Pool " + poolId + " - Fight #" + newTotal);
                }
            } catch (Exception e) {
                System.err.println("Error incrementing fights for pool " + poolId + ": " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
        
        runningPools.put(poolId, future);
        return true;
    }
    
    /**
     * Stop a running 1v1 pool
     * @param poolId The ID of the pool to stop
     * @return true if pool was stopped, false if pool was not running
     */
    public boolean stopPool1v1(String poolId) {
        ScheduledFuture<?> future = runningPools.remove(poolId);
        
        if (future != null) {
            future.cancel(false);
            System.out.println("Stopped pool: " + poolId);
            return true;
        } else {
            System.out.println("Pool " + poolId + " is not running");
            return false;
        }
    }
    
    /**
     * Check if a pool is currently running
     * @param poolId The ID of the pool to check
     * @return true if pool is running, false otherwise
     */
    public boolean isPoolRunning(String poolId) {
        return runningPools.containsKey(poolId);
    }
    
    /**
     * Stop all running pools
     */
    public void stopAllPools() {
        System.out.println("Stopping all running pools...");
        for (String poolId : runningPools.keySet()) {
            stopPool1v1(poolId);
        }
    }
    
    /**
     * Shutdown the pool manager
     */
    public void shutdown() {
        stopAllPools();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        System.out.println("PoolManager shut down");
    }
}
