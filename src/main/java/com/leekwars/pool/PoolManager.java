package com.leekwars.pool;

import com.leekwars.pool.elo.EloManager;
import com.leekwars.pool.leek.PoolRunLeek;
import com.leekwars.pool.run.categories.PoolRunDuel;
import com.leekwars.pool.run.fight.categories.PoolFightDuel;
import com.leekwars.pool.scenarios.categories.PoolScenarioDuel;
import com.leekwars.api.mongo.services.PoolFightDuelService;
import com.leekwars.api.mongo.services.PoolRunDuelService;
import com.leekwars.generator.Generator;
import com.leekwars.generator.outcome.Outcome;
import com.leekwars.generator.scenario.Scenario;
import com.leekwars.generator.test.LocalDbRegisterManager;
import com.leekwars.generator.test.LocalTrophyManager;

import java.util.concurrent.*;
import java.util.List;
import java.util.Map;

public class PoolManager {
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private Map<String, ScheduledFuture<?>> runningPools = new ConcurrentHashMap<>();

    public Generator generator;

    public PoolRunDuelService poolRunDuelService;
    public PoolFightDuelService poolFightDuelService;

    public PoolManager(PoolRunDuelService poolRunDuelService, PoolFightDuelService poolFightDuelService) {
        System.out.println("PoolManager: Init");
        
        this.poolRunDuelService = poolRunDuelService;
        this.poolFightDuelService = poolFightDuelService;
        
        // Without the cache it is 4x slower, need to investigate if it's needed, or if
        // we could only disable cache for the first iteration?
        generator = new Generator();
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

                        // store the fight result

                        PoolFightDuel fight = null;

                        PoolRunLeek leek1 = poolRunDuel.getLeekById(poolScenario.leek1.id);
                        PoolRunLeek leek2 = poolRunDuel.getLeekById(poolScenario.leek2.id);

                        // draw
                        if (outcome.winner == -1) {
                            fight = new PoolFightDuel(poolRunDuel.id, poolScenario.leek1.id, poolScenario.leek2.id, "", scenario.seed);

                            leek1.elo += EloManager.GetRatingDelta(poolScenario.leek1.elo, poolScenario.leek2.elo, 0.5f);
                            leek2.elo += EloManager.GetRatingDelta(poolScenario.leek2.elo, poolScenario.leek1.elo, 0.5f);
                        }
                        // leek1 wins
                        else if (outcome.winner == 0) {
                            fight = new PoolFightDuel(poolRunDuel.id, poolScenario.leek1.id, poolScenario.leek2.id, poolScenario.leek1.id, scenario.seed);
                            
                            leek1.elo += EloManager.GetRatingDelta(poolScenario.leek1.elo, poolScenario.leek2.elo, 1.0f);
                            leek2.elo += EloManager.GetRatingDelta(poolScenario.leek2.elo, poolScenario.leek1.elo, 0.0f);
                        }
                        // leek2 wins
                        else if (outcome.winner == 1) {
                            fight = new PoolFightDuel(poolRunDuel.id, poolScenario.leek1.id, poolScenario.leek2.id, poolScenario.leek2.id, scenario.seed);
                            
                            leek1.elo += EloManager.GetRatingDelta(poolScenario.leek1.elo, poolScenario.leek2.elo, 0.0f);
                            leek2.elo += EloManager.GetRatingDelta(poolScenario.leek2.elo, poolScenario.leek1.elo, 1.0f);
                        }

                        if (fight != null) {
                            this.poolFightDuelService.addPoolFight(fight);

                            // update leeks elo in database
                            boolean updatePoolSuccess = this.poolRunDuelService.updatePoolRunDuel(poolRunDuel.id, poolRunDuel);
                            
                            if (!updatePoolSuccess) {
                                System.err.println("Failed to update leeks elo for PoolRunDuel ID " + poolRunDuel.id);
                            }
                        }

                        fightCount++;

                        // early stop if pool is no longer running
                        if (!isPoolRunning(poolRunDuel.id)) {
                            System.out.println("Pool " + poolRunDuel.id + " has been stopped. Exiting fight loop.");
                            poolRunDuel.stop(true);

                            // update pool run duel status in database
                            boolean updatePoolSuccess = this.poolRunDuelService.updatePoolRunDuel(poolRunDuel.id, poolRunDuel);

                            if (!updatePoolSuccess) {
                                System.err.println("Failed to update PoolRunDuel status for PoolRunDuel ID " + poolRunDuel.id);
                            }

                            // remove pool from runningPools map
                            runningPools.remove(poolRunDuel.id);
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
            poolRunDuel.stop(false);

            // update pool run duel status in database
            boolean updatePoolSuccess = this.poolRunDuelService.updatePoolRunDuel(poolRunDuel.id, poolRunDuel);

            if (!updatePoolSuccess) {
                System.err.println("Failed to update PoolRunDuel status for PoolRunDuel ID " + poolRunDuel.id);
            }

            // remove pool from runningPools map
            runningPools.remove(poolRunDuel.id);
            System.out.println("PoolRunDuel " + poolRunDuel.id + " completed all fights and stopped.");
        }, 0, TimeUnit.SECONDS);

        runningPools.put(poolRunDuel.id, fightRunner);
        return true;
    }

    public boolean poolIsAlreadyRunning(String poolRunId) {
        return runningPools.containsKey(poolRunId);
    }


    /**
     * Stop a running pool run
     * 
     * @param poolRunId
     *            The ID of the pool to stop
     * @return true if pool was stopped, false if pool was not running
     */
    public boolean stopPool(String poolRunId) {
        ScheduledFuture<?> future = runningPools.remove(poolRunId);

        if (future != null) {
            future.cancel(false);
            System.out.println("Stopped pool: " + poolRunId);
            return true;
        } else {
            System.out.println("Pool " + poolRunId + " is not running");
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
        for (String poolRunId : runningPools.keySet()) {
            stopPool(poolRunId);
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
