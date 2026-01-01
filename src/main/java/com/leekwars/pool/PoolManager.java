package com.leekwars.pool;

import com.leekwars.pool.scenarios.ScenarioManager;

public class PoolManager {
    private ScenarioManager scenarioManager = new ScenarioManager();
    public PoolManager() {
        System.out.println("PoolManager: Init");
    }
}
