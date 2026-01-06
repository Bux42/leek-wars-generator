package com.leekwars.pool.scenarios;

import java.util.ArrayList;
import java.util.List;

import com.leekwars.generator.scenario.EntityInfo;
import com.leekwars.generator.scenario.FarmerInfo;
import com.leekwars.generator.scenario.Scenario;
import com.leekwars.generator.scenario.TeamInfo;
import com.leekwars.pool.leek.Leek;

public class ScenarioManager {
    public static FarmerInfo CreateFarmerInfo(int id, String name, String country) {
        FarmerInfo farmer = new FarmerInfo();
        farmer.id = id;
        farmer.name = name;
        farmer.country = country;
        return farmer;
    }

    public static TeamInfo CreateTeamInfo(int id, String name) {
        TeamInfo team = new TeamInfo();
        team.id = id;
        team.name = name;
        return team;
    }

    public static Scenario CreateDuelScenario(int customSeed, Leek leek1, Leek leek2) {
        Scenario scenario = new Scenario();

        FarmerInfo farmer1 = CreateFarmerInfo(1, "None", "fr");
        FarmerInfo farmer2 = CreateFarmerInfo(2, "None", "fr");

        scenario.farmers.put(farmer1.id, farmer1);
        scenario.farmers.put(farmer2.id, farmer2);

        int team1_id = 1;
        int team2_id = 2;

        EntityInfo entity1 = leek1.ToEntityInfo(1, farmer1.id, team1_id);
        EntityInfo entity2 = leek2.ToEntityInfo(2, farmer2.id, team2_id);

        TeamInfo team1 = CreateTeamInfo(team1_id, "None");
        TeamInfo team2 = CreateTeamInfo(team2_id, "None");
        
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
