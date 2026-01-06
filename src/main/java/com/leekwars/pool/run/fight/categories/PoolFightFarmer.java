package com.leekwars.pool.run.fight.categories;

import com.leekwars.pool.run.fight.PoolFightBase;

public class PoolFightFarmer extends PoolFightBase {
    public String farmerId;

    public PoolFightFarmer(String poolRunId, String farmerId, int seed) {
        super(poolRunId, seed, 0);

        this.farmerId = farmerId;
    }
}
