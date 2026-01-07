package com.leekwars.pool.leek;

public class PoolRunLeek extends Leek {
    public String mergedAiCodeHash;

    public PoolRunLeek(Leek leek) {
        this.aiFilePath = leek.aiFilePath;
        this.elo = leek.elo;
        this.id = leek.id;
        this.imageName = leek.imageName;
        this.name = leek.name;
        this.build = leek.build;
    }

    public PoolRunLeek(Leek leek, String mergedAiCodeHash) {
        this.aiFilePath = leek.aiFilePath;
        this.elo = leek.elo;
        this.id = leek.id;
        this.imageName = leek.imageName;
        this.name = leek.name;
        this.build = leek.build;
        
        this.mergedAiCodeHash = mergedAiCodeHash;
    }
}
