package com.leekwars.pool.scenarios.categories;

import com.leekwars.generator.scenario.Scenario;
import com.leekwars.pool.leek.Leek;

public class PoolScenarioDuel {
    public Leek leek1;
    public Leek leek2;
    public Scenario scenario;
    public int seed = 0;

    public PoolScenarioDuel(Leek leek1, Leek leek2, Scenario scenario) {
        this.leek1 = leek1;
        this.leek2 = leek2;
        this.scenario = scenario;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }
}
