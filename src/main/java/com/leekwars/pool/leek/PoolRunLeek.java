package com.leekwars.pool.leek;

import com.alibaba.fastjson.JSONObject;

public class PoolRunLeek extends Leek {
    public String mergedAiCodeHash;

    public PoolRunLeek(Leek leek) {
        this.mergedCodeHash = leek.mergedCodeHash;
        this.elo = leek.elo;
        this.id = leek.id;
        this.imageName = leek.imageName;
        this.name = leek.name;
        this.build = leek.build;
    }

    public static PoolRunLeek fromJson(JSONObject json) {
        Leek leek = Leek.fromJson(json);
        PoolRunLeek poolRunLeek = new PoolRunLeek(leek);
        
        if (json.containsKey("mergedAiCodeHash")) {
            poolRunLeek.mergedAiCodeHash = json.getString("mergedAiCodeHash");
        }
        
        return poolRunLeek;
    }
}
