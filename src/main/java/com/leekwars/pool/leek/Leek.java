package com.leekwars.pool.leek;

import com.leekwars.pool.entity.Entity;

public class Leek extends Entity {
    public int id;
    public int elo = 100;
    public String aiFilePath = "Basic.leek";
    public String imageName = "leekwars/image/leek/leek1_front_green";

    public Leek() {
        super();
    }
}
