package com.leekwars.pool;

import com.leekwars.generator.scenario.Scenario;
import com.leekwars.pool.leek.Leek;
import com.leekwars.pool.scenarios.ScenarioType;

public class Pool {
    public ScenarioType type;
    public Scenario scenario;

    public Pool(ScenarioType type, Scenario scenario, Leek leek1, Leek leek2) {
        this.type = type;
        this.scenario = scenario;
    }
}
