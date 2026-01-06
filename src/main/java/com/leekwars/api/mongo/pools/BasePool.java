package com.leekwars.api.mongo.pools;

public class BasePool {
    public boolean enabled = false;
    public boolean resetElo = true;
    
    public boolean deterministic = false;
    public int startSeed = 0;

    public boolean fightLimitEnabled = true;
    public int fightLimit = 10;

    public String name = "";
    public String id = "";

    public BasePool(String name, String id, boolean enabled, boolean resetElo, boolean fightLimitEnabled, int fightLimit, boolean deterministic, int startSeed) {
        this.name = name;
        this.id = id;
        this.enabled = enabled;
        this.resetElo = resetElo;
        this.fightLimitEnabled = fightLimitEnabled;
        this.fightLimit = fightLimit;
        this.deterministic = deterministic;
        this.startSeed = startSeed;
    }
}
