package com.leekwars.api.mongo.pools.scenarios;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.pools.BasePool;

public class PoolDuel extends BasePool {
    public List<String> leek_ids;

    public PoolDuel(String name, String id, boolean enabled, boolean resetElo, boolean fightLimitEnabled, int fightLimit, boolean deterministic, int startSeed) {
        super(name, id, enabled, resetElo, fightLimitEnabled, fightLimit, deterministic, startSeed);
    }

    /**
     * Create a PoolDuel instance from JSON object
     * 
     * @param json
     *            JSONObject containing pool data
     * @return PoolDuel instance
     */
    public static PoolDuel fromJson(JSONObject json) {
        List<String> leek_ids = new ArrayList<>();
        if (json.containsKey("leek_ids")) {
            JSONArray idsArray = json.getJSONArray("leek_ids");
            for (int i = 0; i < idsArray.size(); i++) {
                leek_ids.add(idsArray.getString(i));
            }
        }

        String name = json.getString("name");
        String id = json.getString("id");
        boolean enabled = json.getBooleanValue("enabled");
        boolean resetElo = json.getBooleanValue("resetElo");
        boolean fightLimitEnabled = json.getBooleanValue("fightLimitEnabled");
        int fightLimit = json.getIntValue("fightLimit");
        boolean deterministic = json.getBooleanValue("deterministic");
        int startSeed = json.getIntValue("startSeed");

        PoolDuel pool = new PoolDuel(name, id, enabled, resetElo, fightLimitEnabled, fightLimit, deterministic, startSeed);
        pool.leek_ids = leek_ids;
        
        if (json.containsKey("enabled")) {
            pool.enabled = json.getBooleanValue("enabled");
        }

        return pool;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("id", this.id);
        json.put("enabled", this.enabled);
        json.put("resetElo", this.resetElo);
        json.put("fightLimitEnabled", this.fightLimitEnabled);
        json.put("fightLimit", this.fightLimit);
        json.put("deterministic", this.deterministic);
        json.put("startSeed", this.startSeed);

        JSONArray idsArray = new JSONArray();
        for (String leek_id : this.leek_ids) {
            idsArray.add(leek_id);
        }
        json.put("leek_ids", idsArray);

        return json;
    }
}
