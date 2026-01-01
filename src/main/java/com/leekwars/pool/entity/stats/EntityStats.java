package com.leekwars.pool.entity.stats;

import com.alibaba.fastjson.JSONObject;

public class EntityStats {
    public int life;
    public int strength;
    public int wisdom;
    public int agility;
    public int resistance;
    public int science;
    public int magic;
    public int frequency;
    public int cores;
    public int ram;
    public int tp;
    public int mp;

    /**
     * Create an EntityStats instance from JSON object
     * @param json JSONObject containing entity stats data
     * @return EntityStats instance
     */
    public static EntityStats fromJson(JSONObject json) {
        if (json == null) {
            return new EntityStats();
        }
        
        EntityStats stats = new EntityStats();
        
        if (json.containsKey("life")) stats.life = json.getIntValue("life");
        if (json.containsKey("strength")) stats.strength = json.getIntValue("strength");
        if (json.containsKey("wisdom")) stats.wisdom = json.getIntValue("wisdom");
        if (json.containsKey("agility")) stats.agility = json.getIntValue("agility");
        if (json.containsKey("resistance")) stats.resistance = json.getIntValue("resistance");
        if (json.containsKey("science")) stats.science = json.getIntValue("science");
        if (json.containsKey("magic")) stats.magic = json.getIntValue("magic");
        if (json.containsKey("frequency")) stats.frequency = json.getIntValue("frequency");
        if (json.containsKey("cores")) stats.cores = json.getIntValue("cores");
        if (json.containsKey("ram")) stats.ram = json.getIntValue("ram");
        if (json.containsKey("tp")) stats.tp = json.getIntValue("tp");
        if (json.containsKey("mp")) stats.mp = json.getIntValue("mp");
        
        return stats;
    }
}
