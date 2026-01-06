package com.leekwars.pool.elo;

public class EloManager {
    public static float GetRatingDelta(int elo1, int elo2, float gameResult) {
        var myChanceToWin = 1 / (1 + Math.pow(10, (elo2 - elo1) / 400));

        float elo = Math.round(32 * (gameResult - myChanceToWin));

        return elo;
    }
}
