package com.leekwars.pool;

import com.leekwars.pool.elo.EloManager;
import com.leekwars.pool.run.categories.PoolRunDuel;
import com.leekwars.pool.run.fight.categories.PoolFightDuo;
import com.leekwars.pool.scenarios.ScenarioManager;
import com.leekwars.pool.scenarios.categories.PoolScenarioDuel;
import com.leekwars.api.mongo.MongoDbManager;
import com.leekwars.api.mongo.pools.scenarios.PoolDuel;
import com.leekwars.generator.Generator;
import com.leekwars.generator.outcome.Outcome;
import com.leekwars.generator.scenario.Scenario;
import com.leekwars.generator.test.LocalDbRegisterManager;
import com.leekwars.generator.test.LocalTrophyManager;

import java.util.concurrent.*;
import java.util.List;
import java.util.Map;

public class PoolManager {
    private ScenarioManager scenarioManager = new ScenarioManager();
    private MongoDbManager mongoDbManager;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private Map<String, ScheduledFuture<?>> runningPools = new ConcurrentHashMap<>();

    public Generator generator = new Generator();

    public PoolManager(MongoDbManager mongoDbManager) {
        this.mongoDbManager = mongoDbManager;
        System.out.println("PoolManager: Init");

        // Without the cache it is 4x slower, need to investigate if it's needed, or if
        // we could only disable cache for the first iteration?
        generator.setCache(true);
    }

    public boolean startPoolDuel(PoolRunDuel poolRunDuel) {
        List<PoolScenarioDuel> scenarios = poolRunDuel.generateAllScenarios();
        
        ScheduledFuture<?> fightRunner = scheduler.schedule(() -> {
            int fightCount = 0;

            if (poolRunDuel.pool.fightLimitEnabled) {
                for (int iteration = 0; iteration < poolRunDuel.pool.fightLimit; iteration++) {
                    System.out.println("PoolManager: Starting iteration " + (iteration + 1) + "/" + poolRunDuel.pool.fightLimit);
                    // get time start
                    long startTime = System.currentTimeMillis();

                    for (PoolScenarioDuel poolScenario : scenarios) {
                        Scenario scenario = poolScenario.scenario;

                        // set seed if deterministic
                        if (poolRunDuel.pool.deterministic) {
                            scenario.seed = fightCount + 1;
                        }

                        Outcome outcome = generator.runScenario(scenario, null, new LocalDbRegisterManager(), new LocalTrophyManager());

                        System.out.println("Winner: " + outcome.winner);

                        // store the fight result
                        PoolFightDuo fight = null;
                        float delta1 = 0;
                        float delta2 = 0;

                        // draw
                        if (outcome.winner == -1) {
                            fight = new PoolFightDuo(poolRunDuel.id, poolScenario.leek1.id, poolScenario.leek2.id, "", scenario.seed);

                            delta1 = EloManager.GetRatingDelta(poolScenario.leek1.elo, poolScenario.leek2.elo, 0.5f);
                            delta2 = EloManager.GetRatingDelta(poolScenario.leek2.elo, poolScenario.leek1.elo, 0.5f);
                        }
                        // leek1 wins
                        else if (outcome.winner == 0) {
                            fight = new PoolFightDuo(poolRunDuel.id, poolScenario.leek1.id, poolScenario.leek2.id, poolScenario.leek1.id, scenario.seed);
                            
                            delta1 = EloManager.GetRatingDelta(poolScenario.leek1.elo, poolScenario.leek2.elo, 1.0f);
                            delta2 = EloManager.GetRatingDelta(poolScenario.leek2.elo, poolScenario.leek1.elo, 0.0f);
                        }
                        // leek2 wins
                        else if (outcome.winner == 1) {
                            fight = new PoolFightDuo(poolRunDuel.id, poolScenario.leek1.id, poolScenario.leek2.id, poolScenario.leek2.id, scenario.seed);
                            
                            delta1 = EloManager.GetRatingDelta(poolScenario.leek1.elo, poolScenario.leek2.elo, 0.0f);
                            delta2 = EloManager.GetRatingDelta(poolScenario.leek2.elo, poolScenario.leek1.elo, 1.0f);
                        }

                        if (fight != null) {
                            mongoDbManager.addFightItem(fight);

                            poolScenario.leek1.elo += delta1;
                            poolScenario.leek2.elo += delta2;

                            // update leeks elo in database
                            mongoDbManager.updateFightDuoLeeksElo(poolRunDuel.id, poolScenario.leek1, poolScenario.leek1.elo, poolScenario.leek2, poolScenario.leek2.elo);
                        }

                        fightCount++;

                        // early stop if pool is no longer running
                        if (!isPoolRunning(poolRunDuel.pool.id)) {
                            System.out.println("Pool " + poolRunDuel.pool.id + " has been stopped. Exiting fight loop.");
                            poolRunDuel.stop(mongoDbManager, true);
                            // remove pool from runningPools map
                            runningPools.remove(poolRunDuel.pool.id);
                            return;
                        }
                    }

                    // get time end
                    long endTime = System.currentTimeMillis();
                    System.out.println("Iteration " + (iteration + 1) + " took " + (endTime - startTime) + " ms");
                }
            } else {
                // unlimited fights until stopped
            }
            poolRunDuel.stop(mongoDbManager, false);
            // remove pool from runningPools map
            runningPools.remove(poolRunDuel.pool.id);
            System.out.println("Pool " + poolRunDuel.pool.id + " completed all fights and stopped.");
        }, 0, TimeUnit.SECONDS);

        runningPools.put(poolRunDuel.pool.id, fightRunner);

        return true;
    }

    public boolean poolIsAlreadyRunning(String poolId) {
        return runningPools.containsKey(poolId);
    }


    /**
     * Stop a running pool
     * 
     * @param poolId
     *            The ID of the pool to stop
     * @return true if pool was stopped, false if pool was not running
     */
    public boolean stopPool(String poolId) {
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
     * 
     * @param poolId
     *            The ID of the pool to check
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
            stopPool(poolId);
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
