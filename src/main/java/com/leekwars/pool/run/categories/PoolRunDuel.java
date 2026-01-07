package com.leekwars.pool.run.categories;

import java.util.List;
import com.leekwars.pool.scenarios.ScenarioManager;
import com.leekwars.pool.scenarios.categories.PoolScenarioDuel;
import com.leekwars.generator.scenario.Scenario;
import com.leekwars.pool.BasePool;
import com.leekwars.pool.leek.PoolRunLeek;
import com.leekwars.pool.run.PoolRunBase;

public class PoolRunDuel extends PoolRunBase {
    // store a snapshot of leeks at the start of the pool
    public List<PoolRunLeek> leeks;

    public PoolRunDuel(BasePool base, List<PoolRunLeek> leeks) {
        super(base);
        super.init();

        SetLeeks(leeks);
    }

    public void SetLeeks(List<PoolRunLeek> leeks) {
        this.leeks = leeks;

        // reset elo of all leeks if needed
        if (this.pool.resetElo) {
            resetLeeksElo();
        }
    }

    public void resetLeeksElo() {
        for (PoolRunLeek leek : this.leeks) {
            leek.elo = 100;
        }
    }

    public void stop(boolean interrupted) {
        super.stop(interrupted);
    }

    public PoolRunLeek getLeekById(String leekId) {
        for (PoolRunLeek leek : leeks) {
            if (leek.id.equals(leekId)) {
                return leek;
            }
        }
        return null;
    }

    public List<PoolScenarioDuel> generateAllScenarios() {
        List<PoolScenarioDuel> scenarios = new java.util.ArrayList<>();

        for (int i = 0; i < leeks.size(); i++) {
            for (int j = 0; j < leeks.size(); j++) {
                // avoid fighting oneself
                if (i != j) {
                    // generate scenario, with customSeed set to 0 for now, it will be set when
                    // executing the actual fights if needed
                    Scenario duelScenario = ScenarioManager.CreateDuelScenario(0, leeks.get(i), leeks.get(j));
                    PoolScenarioDuel poolScenarioDuel = new PoolScenarioDuel(leeks.get(i), leeks.get(j), duelScenario);

                    scenarios.add(poolScenarioDuel);
                }
            }
        }

        return scenarios;
    }

}
