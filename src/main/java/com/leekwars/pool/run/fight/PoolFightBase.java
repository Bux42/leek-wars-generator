package com.leekwars.pool.run.fight;

import com.alibaba.fastjson.JSONObject;

public class PoolFightBase {
    public String poolRunId = "";
    public int seed = 0;
    public int type = 0;
    public long date;
    public String id = "";

    public PoolFightBase(String poolRunId, int seed, int type) {
        this.poolRunId = poolRunId;
        this.seed = seed;
        this.type = type;
        this.date = System.currentTimeMillis();
    }

    public static PoolFightBase fromJson(JSONObject json) {
        PoolFightBase fight = new PoolFightBase("", 0, 0);

        if (json.containsKey("poolRunId")) {
            fight.poolRunId = json.getString("poolRunId");
        }
        if (json.containsKey("seed")) {
            fight.seed = json.getIntValue("seed");
        }
        if (json.containsKey("type")) {
            fight.type = json.getIntValue("type");
        }
        if (json.containsKey("date")) {
            fight.date = json.getLongValue("date");
        }

        // If json comes from MongoDB, _id is an object with $oid field
        if (json.containsKey("_id")) {
            fight.id = json.getJSONObject("_id").getString("$oid").toString();
        }

        // else, it's from the frontend API, so id is a string field
        else if (json.containsKey("id")) {
            fight.id = json.getString("id");
        }

        return fight;
    }
}