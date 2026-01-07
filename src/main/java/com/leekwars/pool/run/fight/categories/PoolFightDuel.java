package com.leekwars.pool.run.fight.categories;

import com.leekwars.pool.run.fight.PoolFightBase;

public class PoolFightDuel extends PoolFightBase {
    public String leek1Id;
    public String leek2Id;
    public String winnerLeekId = "";

    public PoolFightDuel(String poolRunId, String leek1Id, String leek2Id, String winnerLeekId, int seed) {
        super(poolRunId, seed, 0);

        this.leek1Id = leek1Id;
        this.leek2Id = leek2Id;
        this.winnerLeekId = winnerLeekId;
    }
}
