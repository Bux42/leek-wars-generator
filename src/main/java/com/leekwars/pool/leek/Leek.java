package com.leekwars.pool.leek;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.generator.scenario.EntityInfo;
import com.leekwars.pool.builds.EntityBuild;
import com.leekwars.pool.entity.Entity;
import com.leekwars.pool.entity.stats.EntityStats;

public class Leek extends Entity {
    public String id;
    public int elo = 100;
    public String mergedCodeHash = null;
    public String imageName = "leekwars/image/leek/leek1_front_green";

    public Leek() {
        super();
    }

    /**
     * Create a Leek instance from JSON object
     * @param json JSONObject containing leek data
     * @return Leek instance
     */
    public static Leek fromJson(JSONObject json) {
        Leek leek = new Leek();
        
        // Set name from parent Entity class
        if (json.containsKey("name")) {
            leek.name = json.getString("name");
        }
        
        // Set build from parent Entity class
        if (json.containsKey("build")) {
            leek.build = EntityBuild.fromJson(json.getJSONObject("build"));
        }
        
        // Set Leek specific fields

        // If json comes from MongoDB, _id is an object with $oid field
        if (json.containsKey("_id")) {
            leek.id = json.getJSONObject("_id").getString("$oid").toString();
        }

        // else, it's from the frontend API, so id is a string field
        else if (json.containsKey("id")) {
            leek.id = json.getString("id");
        }
        
        if (json.containsKey("elo")) {
            leek.elo = json.getIntValue("elo");
        }
        
        if (json.containsKey("mergedCodeHash")) {
            leek.mergedCodeHash = json.getString("mergedCodeHash");
        }
        
        if (json.containsKey("imageName")) {
            leek.imageName = json.getString("imageName");
        }
        
        return leek;
    }

    public EntityInfo ToEntityInfo(int id, int farmer_id, int team_id) {
        EntityInfo entity = new EntityInfo();
        entity.id = id;
        entity.name = name;

        String mergedCodePath = ".merged_ais/" + mergedCodeHash + ".leek";
        String relativeAiFilePath = LeekManager.GetSanitizedRelativeAiFilePath(mergedCodePath);
        entity.ai = relativeAiFilePath; 

        entity.farmer = farmer_id;
        entity.team = team_id;
        entity.type = 0; // leek

        entity.level = build.level;

        EntityStats totalStats = this.build.getTotalStats();

        entity.life = totalStats.life;
        entity.strength = totalStats.strength;
        entity.wisdom = totalStats.wisdom;
        entity.agility = totalStats.agility;
        entity.resistance = totalStats.resistance;
        entity.science = totalStats.science;
        entity.magic = totalStats.magic;
        entity.frequency = totalStats.frequency;
        entity.cores = totalStats.cores;
        entity.ram = totalStats.ram;
        entity.tp = totalStats.tp;
        entity.mp = totalStats.mp;

        entity.weapons = Arrays.stream(build.selectedWeaponIds).boxed().collect(Collectors.toList());
        entity.chips = Arrays.stream(build.selectedChipIds).boxed().collect(Collectors.toList());
        return entity;
    }
}
