package com.leekwars.pool.run.fight.talent;

public class TalentDataPoint {
    public String entityName;
    public int talent;
    public long timestamp;

    public TalentDataPoint(long date, String leekId, int leek1Elo) {
        this.timestamp = date;
        this.entityName = leekId;
        this.talent = leek1Elo;
    }
}
