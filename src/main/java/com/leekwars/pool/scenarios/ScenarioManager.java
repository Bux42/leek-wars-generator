package com.leekwars.pool.scenarios;

import java.util.ArrayList;
import java.util.List;

import com.leekwars.generator.scenario.EntityInfo;
import com.leekwars.generator.scenario.FarmerInfo;
import com.leekwars.generator.scenario.Scenario;
import com.leekwars.generator.scenario.TeamInfo;
import com.leekwars.pool.leek.Leek;

public class ScenarioManager {
    public Scenario Create1v1Scenario(int customSeed, Leek leek1, Leek leek2) {
        Scenario scenario = new Scenario();

        FarmerInfo farmer1 = new FarmerInfo();
        farmer1.id = 1;
        farmer1.name = "None";
        farmer1.country = "fr";

        FarmerInfo farmer2 = new FarmerInfo();
        farmer2.id = 2;
        farmer2.name = "None";
        farmer2.country = "fr";

        int team1_id = 1;
        int team2_id = 2;

        scenario.farmers.put(farmer1.id, farmer1);
        scenario.farmers.put(farmer2.id, farmer2);

        EntityInfo entity1 = leek1.ToEntityInfo(1, farmer1.id, team1_id);
        EntityInfo entity2 = leek2.ToEntityInfo(2, farmer2.id, team2_id);

        TeamInfo team1 = new TeamInfo();
        team1.id = 1;
        team1.name = "None";

        TeamInfo team2 = new TeamInfo();
        team2.id = 2;
        team2.name = "None";

        scenario.teams.put(team1.id, team1);
        scenario.teams.put(team2.id, team2);

        List<EntityInfo> team1Entities = new ArrayList<EntityInfo>();
        team1Entities.add(entity1);

        List<EntityInfo> team2Entities = new ArrayList<EntityInfo>();
        team2Entities.add(entity2);

        scenario.entities.add(team1Entities);
        scenario.entities.add(team2Entities);

        scenario.seed = customSeed;
        return scenario;
    }
}
