package com.leekwars.api.mongo.scenarios;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class PoolOneVersusOne {
    public List<String> leek_ids;
    public String name;
    public String id;
    public boolean enabled = true;

    // stats after running the pool
    public int total_fights = 0;

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
            pool.total_fights = json.getIntValue("total_fights");
        }
        
        return pool;
    }
}
