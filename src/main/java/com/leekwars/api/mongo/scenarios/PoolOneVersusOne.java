package com.leekwars.api.mongo.scenarios;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.generator.scenario.Scenario;
import com.leekwars.pool.leek.Leek;
import com.leekwars.pool.scenarios.OneVersusOneScenario;
import com.leekwars.pool.scenarios.ScenarioManager;

import java.util.ArrayList;
import java.util.List;

public class PoolOneVersusOne {
    private List<Leek> leeks;
    private List<OneVersusOneScenario> scenarios;
    public List<String> leek_ids;
    public String name;
    public String id;
    public boolean enabled = true;

    public boolean fight_count_limit_enabled = false;
    public int fight_count_limit = 10;

    // stats after running the pool
    public int total_executed_fights = 0;

    public PoolOneVersusOne(List<String> leek_ids, String name, String id) {
        this.leek_ids = leek_ids;
        this.name = name;
        this.id = id;
    }

    /**
     * Create a PoolOneVersusOne instance from JSON object
     * @param json JSONObject containing pool data
     * @return PoolOneVersusOne instance
     */
    public static PoolOneVersusOne fromJson(JSONObject json) {
        List<String> leek_ids = new ArrayList<>();
        if (json.containsKey("leek_ids")) {
            JSONArray idsArray = json.getJSONArray("leek_ids");
            for (int i = 0; i < idsArray.size(); i++) {
                leek_ids.add(idsArray.getString(i));
            }
        }
        
        String name = json.getString("name");
        String id = json.getString("id");
        
        PoolOneVersusOne pool = new PoolOneVersusOne(leek_ids, name, id);
        
        if (json.containsKey("enabled")) {
            pool.enabled = json.getBooleanValue("enabled");
        }
        
        if (json.containsKey("total_fights")) {
            pool.total_executed_fights = json.getIntValue("total_fights");
        }
        
        return pool;
    }

    public void SetLeeks(List<Leek> leeks) {
        this.leeks = leeks;
    }

    public void GenerateAllMatchupsScenarios(List<Leek> leeks) {
        // ArrayList<PoolScenario> poolScenarios = new ArrayList<>();
        scenarios = new ArrayList<>();
        for (int i = 0; i < leeks.size(); i++) {
            for (int j = 0; j < leeks.size(); j++) {
                if (i != j) {
                    Scenario scenario = ScenarioManager.Create1v1Scenario(0, leeks.get(i), leeks.get(j));
                    OneVersusOneScenario oneVsOneScenario = new OneVersusOneScenario(leeks.get(i), leeks.get(j), scenario);
                    scenarios.add(oneVsOneScenario);
                }
            }
        }
    }

    public List<OneVersusOneScenario> GetScenarios() {
        return scenarios;
    }
}
