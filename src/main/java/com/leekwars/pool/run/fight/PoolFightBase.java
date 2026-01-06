package com.leekwars.pool.run.fight;

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
        this.id = java.util.UUID.randomUUID().toString();
    }
}