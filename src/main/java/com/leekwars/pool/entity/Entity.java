package com.leekwars.pool.entity;

import com.alibaba.fastjson.JSONObject;
import com.leekwars.pool.builds.EntityBuild;

public class Entity {
    public EntityBuild build;
    public String name = "Default Entity";

    public Entity() {
        build = new EntityBuild();
    }

    /**
     * Create an Entity instance from JSON object
     * @param json JSONObject containing entity data
     * @return Entity instance
     */
    public static Entity fromJson(JSONObject json) {
        if (json == null) {
            return new Entity();
        }
        
        Entity entity = new Entity();
        
        if (json.containsKey("name")) {
            entity.name = json.getString("name");
        }
        
        if (json.containsKey("build")) {
            entity.build = EntityBuild.fromJson(json.getJSONObject("build"));
        }
        
        return entity;
    }
}
