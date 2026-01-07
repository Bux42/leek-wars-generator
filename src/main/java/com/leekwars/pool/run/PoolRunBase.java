package com.leekwars.pool.run;

import com.leekwars.pool.BasePool;

public abstract class PoolRunBase {
    public boolean running = false;
    public boolean interrupted = false;

    public long startTime = 0;
    public long endTime = 0;

    public BasePool pool;
    public String id = "";

    public PoolRunBase(BasePool pool) {
        this.pool = pool;
    }

    public void init() {
        this.running = true;
        this.startTime = System.currentTimeMillis();
    }

    public void stop(boolean interrupted) {
        this.running = false;
        this.endTime = System.currentTimeMillis();
        this.interrupted = interrupted;
    }

    public void setRunId(String run_id) {
        this.id = run_id;   
    }
}
