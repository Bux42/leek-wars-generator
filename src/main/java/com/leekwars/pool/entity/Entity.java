package com.leekwars.pool.entity;

import com.leekwars.pool.builds.EntityBuild;

public class Entity {
    public EntityBuild build;
    public String name = "Default Entity";

    public Entity() {
        build = new EntityBuild();
    }
}
