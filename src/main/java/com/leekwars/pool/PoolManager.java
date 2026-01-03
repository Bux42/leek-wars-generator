package com.leekwars.pool;

import com.leekwars.pool.scenarios.OneVersusOneScenario;
import com.leekwars.pool.scenarios.ScenarioManager;
import com.leekwars.api.mongo.MongoDbManager;
import com.leekwars.api.mongo.scenarios.PoolOneVersusOne;
import com.leekwars.generator.Generator;
import com.leekwars.generator.outcome.Outcome;
import com.leekwars.generator.scenario.Scenario;
import com.leekwars.generator.test.LocalDbRegisterManager;
import com.leekwars.generator.test.LocalTrophyManager;
import com.leekwars.pool.leek.Leek;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.bson.Document;
import java.util.concurrent.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

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
        
        // Get all Leek objects from the pool's leek_ids
        List<Leek> leeks = new ArrayList<>();
        for (String leekId : pool.leek_ids) {
            Document leekDoc = mongoDbManager.getLeekById(leekId);
            if (leekDoc != null) {
                String leekJson = leekDoc.toJson();
                JSONObject leekObject = JSON.parseObject(leekJson);
                leekObject.remove("_id");
                Leek leek = Leek.fromJson(leekObject);
                leeks.add(leek);
            } else {
                System.err.println("Warning: Leek with ID " + leekId + " not found in database");
            }
        }
        
        if (leeks.isEmpty()) {
            System.err.println("Pool " + poolId + " has no valid leeks");
            return false;
        }
        
        System.out.println("Starting pool: " + pool.name + " (ID: " + poolId + ") with " + leeks.size() + " leeks");

        PoolOneVersusOne currentPool = mongoDbManager.getPool1v1ById(poolId);

        if (currentPool == null) {
            System.err.println("Pool " + poolId + " not found in database");
            return false;
        }

        currentPool.SetLeeks(leeks);

        currentPool.GenerateAllMatchupsScenarios(leeks);

        // reset total_executed_fights to 0
        org.bson.Document resetDoc = new org.bson.Document("total_executed_fights", 0);
        mongoDbManager.updatePool1v1(poolId, resetDoc);

        Generator generator = new Generator();

        // Without the cache it is 4x slower, need to investigate if it's needed, or if we could only disable cache for the first iteration?
        // generator.setCache(false);

        // Run all scenarios fights in background
        ScheduledFuture <?> fightRunner = scheduler.schedule(() -> {
            int fightCount = 0;

            for (int iteration = 0; iteration < currentPool.fight_count_limit; iteration++) {
				System.out.println("PoolManager: Starting iteration " + (iteration + 1) + "/" + currentPool.fight_count_limit);
                // get time start
                long startTime = System.currentTimeMillis();
				for (OneVersusOneScenario poolScenario : currentPool.GetScenarios()) {
                    Scenario scenario = poolScenario.scenario;
					// if (deterministic) {
					// 	scenario.seed = iteration + 1;
					// } else if (poolManager.customSeed != 0) {
					// 	scenario.seed = poolManager.customSeed;
					// }
					Outcome outcome = generator.runScenario(scenario, null, new LocalDbRegisterManager(),
							new LocalTrophyManager());

                    // System.out.println(JSON.toJSONString(outcome.toJson(), false));
					System.out.println("Scenario: " + poolScenario.leek1.name + " vs " + poolScenario.leek2.name + " => Winner: " + outcome.winner);

					poolScenario.onWinner(outcome, poolScenario.leek1, poolScenario.leek2);
					fightCount++;

                    // early stop if pool is no longer running
                    if (!isPoolRunning(poolId)) {
                        System.out.println("Pool " + poolId + " has been stopped. Exiting fight loop.");
                        return;
                    }
				}

                // get time end
                long endTime = System.currentTimeMillis();
                System.out.println("Iteration " + (iteration + 1) + " took " + (endTime - startTime) + " ms");

                // update total_executed_fights in database
                org.bson.Document updates = new org.bson.Document("total_executed_fights", fightCount);
                mongoDbManager.updatePool1v1(poolId, updates);

                // update leeks elo in database
                for (Leek leek : leeks) {
                    org.bson.Document leekUpdates = new org.bson.Document("elo", leek.elo);
                    System.out.println("Updating leek " + leek.id + " elo to " + leek.elo);
                    mongoDbManager.updateLeek(leek.id, leekUpdates);
                }
			}
            // remove pool from runningPools map
            runningPools.remove(poolId);
            System.out.println("Pool " + poolId + " completed all fights and stopped.");
        }, 0, TimeUnit.SECONDS);
        

        runningPools.put(poolId, fightRunner);
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
