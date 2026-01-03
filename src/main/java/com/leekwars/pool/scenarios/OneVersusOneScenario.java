package com.leekwars.pool.scenarios;

import com.leekwars.generator.outcome.Outcome;
import com.leekwars.generator.scenario.Scenario;
import com.leekwars.pool.leek.Leek;

public class OneVersusOneScenario {
    public Leek leek1;
    public Leek leek2;
    public Scenario scenario;

    public OneVersusOneScenario(Leek leek1, Leek leek2, Scenario scenario) {
        this.leek1 = leek1;
        this.leek2 = leek2;
        this.scenario = scenario;
    }

    public float GetRatingDelta(Leek myLeek, Leek opponentLeek, float myGameResult) {
        var myChanceToWin = 1 / (1 + Math.pow(10, (opponentLeek.elo - myLeek.elo) / 400));

        float elo = Math.round(32 * (myGameResult - myChanceToWin));

        return elo;
    }

    public void onWinner(Outcome outcome, Leek leek1, Leek leek2) {
        if (outcome.winner == 0) {
            // leek1 wins
            // leek1.wins++;
            // leek2.losses++;
            float delta1 = GetRatingDelta(leek1, leek2, 1);
            float delta2 = GetRatingDelta(leek2, leek1, 0);

            // System.out.println(">>>> leek1 elo: " + leek1.elo + " | leek2 elo: " +
            // leek2.elo);
            // System.out.println(">>>> leek1 delta: " + delta1 + " | leek2 delta: " +
            // delta2);
            leek1.elo += delta1;
            leek2.elo += delta2;
        } else if (outcome.winner == 1) {
            // leek2 wins
            // leek2.wins++;
            // leek1.losses++;
            float delta1 = GetRatingDelta(leek1, leek2, 0);
            float delta2 = GetRatingDelta(leek2, leek1, 1);
            leek1.elo += delta1;
            leek2.elo += delta2;
        } else {
            // draw
            // leek1.draws++;
            // leek2.draws++;
            float delta1 = GetRatingDelta(leek1, leek2, 0.5f);
            float delta2 = GetRatingDelta(leek2, leek1, 0.5f);
            leek1.elo += delta1;
            leek2.elo += delta2;
        }
        // leek1.total_fights++;
        // leek2.total_fights++;
    }
}
