package com.leekwars.pool.categories;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.pool.BasePool;

public class PoolDuel extends BasePool {
    public List<String> leekIds;

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
        List<String> leekIds = new ArrayList<>();
        if (json.containsKey("leekIds")) {
            JSONArray idsArray = json.getJSONArray("leekIds");
            for (int i = 0; i < idsArray.size(); i++) {
                leekIds.add(idsArray.getString(i));
            }
        }

        String name = json.getString("name");

        String id = null;

        // check if it's a mongo id
        if (json.containsKey("_id")) {
            id = json.getJSONObject("_id").getString("$oid");
        }
        // or a regular id from front end
        if (json.containsKey("id")) {
            id = json.getString("id");
        }

        boolean enabled = json.getBooleanValue("enabled");
        boolean resetElo = json.getBooleanValue("resetElo");
        boolean fightLimitEnabled = json.getBooleanValue("fightLimitEnabled");
        int fightLimit = json.getIntValue("fightLimit");
        boolean deterministic = json.getBooleanValue("deterministic");
        int startSeed = json.getIntValue("startSeed");

        PoolDuel pool = new PoolDuel(name, id, enabled, resetElo, fightLimitEnabled, fightLimit, deterministic, startSeed);
        pool.leekIds = leekIds;
        
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
        for (String leek_id : this.leekIds) {
            idsArray.add(leek_id);
        }
        json.put("leekIds", idsArray);

        return json;
    }
}
