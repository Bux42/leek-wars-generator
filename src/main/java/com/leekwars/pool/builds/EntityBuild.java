package com.leekwars.pool.builds;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.pool.entity.stats.EntityStats;

public class EntityBuild {
    public int level = 1;
    public EntityStats investedStats;
    public int investedCapital;
    public EntityStats bonusStats;
    public int[] equippedComponentIds;
    public int[] selectedWeaponIds;
    public int[] selectedChipIds;
    public int totalCapital = 50;

    public EntityBuild() {
        investedStats = new EntityStats();
        bonusStats = new EntityStats();
        equippedComponentIds = new int[0];
        selectedWeaponIds = new int[0];
        selectedChipIds = new int[0];
    }

    /**
     * Create an EntityBuild instance from JSON object
     * @param json JSONObject containing build data
     * @return EntityBuild instance
     */
    public static EntityBuild fromJson(JSONObject json) {
        if (json == null) {
            return new EntityBuild();
        }
        
        EntityBuild build = new EntityBuild();
        
        if (json.containsKey("level")) {
            build.level = json.getIntValue("level");
        }
        
        if (json.containsKey("investedCapital")) {
            build.investedCapital = json.getIntValue("investedCapital");
        }
        
        if (json.containsKey("totalCapital")) {
            build.totalCapital = json.getIntValue("totalCapital");
        }
        
        if (json.containsKey("investedStats")) {
            build.investedStats = EntityStats.fromJson(json.getJSONObject("investedStats"));
        }
        
        if (json.containsKey("bonusStats")) {
            build.bonusStats = EntityStats.fromJson(json.getJSONObject("bonusStats"));
        }
        
        if (json.containsKey("equippedComponentIds")) {
            JSONArray array = json.getJSONArray("equippedComponentIds");
            build.equippedComponentIds = new int[array.size()];
            for (int i = 0; i < array.size(); i++) {
                build.equippedComponentIds[i] = array.getIntValue(i);
            }
        }
        
        if (json.containsKey("selectedWeaponIds")) {
            JSONArray array = json.getJSONArray("selectedWeaponIds");
            build.selectedWeaponIds = new int[array.size()];
            for (int i = 0; i < array.size(); i++) {
                build.selectedWeaponIds[i] = array.getIntValue(i);
            }
        }
        
        if (json.containsKey("selectedChipIds")) {
            JSONArray array = json.getJSONArray("selectedChipIds");
            build.selectedChipIds = new int[array.size()];
            for (int i = 0; i < array.size(); i++) {
                build.selectedChipIds[i] = array.getIntValue(i);
            }
        }
        
        return build;
    }

    public EntityStats getTotalStats() {
        EntityStats total = new EntityStats();

        // setup base stats for lvl 1 leek
        total.life = 100 + (level - 1) * 3;
        total.frequency = 100;
        total.cores = 1;
        total.ram = 6;
        total.tp = 10;
        total.mp = 3;

        // add invested stats
        total.life += investedStats.life;
        total.strength += investedStats.strength;
        total.wisdom += investedStats.wisdom;
        total.agility += investedStats.agility;
        total.resistance += investedStats.resistance;
        total.science += investedStats.science;
        total.magic += investedStats.magic;
        total.frequency += investedStats.frequency;
        total.cores += investedStats.cores;
        total.ram += investedStats.ram;
        total.tp += investedStats.tp;
        total.mp += investedStats.mp;

        // add bonus stats
        total.life += bonusStats.life;
        total.strength += bonusStats.strength;
        total.wisdom += bonusStats.wisdom;
        total.agility += bonusStats.agility;
        total.resistance += bonusStats.resistance;
        total.science += bonusStats.science;
        total.magic += bonusStats.magic;
        total.frequency += bonusStats.frequency;
        total.cores += bonusStats.cores;
        total.ram += bonusStats.ram;
        total.tp += bonusStats.tp;
        total.mp += bonusStats.mp;

        return total;
    }
}
