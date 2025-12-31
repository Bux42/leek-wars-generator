package com.leekwars.pool.builds;

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
}
