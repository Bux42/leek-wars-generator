package com.leekwars.pool.run.categories;

import java.util.List;
import com.leekwars.pool.scenarios.ScenarioManager;
import com.leekwars.pool.scenarios.categories.PoolScenarioDuel;
import com.alibaba.fastjson.JSONObject;
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

    public static PoolRunDuel fromJson(JSONObject json) {
        List<PoolRunLeek> leeks = new java.util.ArrayList<>();

        for (Object obj : json.getJSONArray("leeks")) {
            JSONObject leekJson = (JSONObject) obj;
            PoolRunLeek leek = PoolRunLeek.fromJson(leekJson);
            leeks.add(leek);
        }

        PoolRunDuel poolRunDuel = new PoolRunDuel(
            BasePool.fromJson(json.getJSONObject("pool")),
            leeks
        );

        // from front API, id is a string field
        if (json.containsKey("id")) {
            poolRunDuel.id = json.getString("id");
        }

        // from mongoDB, _id is an object with $oid field
        else if (json.containsKey("_id")) {
            poolRunDuel.id = json.getJSONObject("_id").getString("$oid").toString();
        }

        poolRunDuel.startTime = json.getLongValue("startTime");
        poolRunDuel.endTime = json.getLongValue("endTime");
        poolRunDuel.running = json.getBooleanValue("running");
        poolRunDuel.interrupted = json.getBooleanValue("interrupted");

        return poolRunDuel;
    }
}
