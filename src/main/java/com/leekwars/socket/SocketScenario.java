package com.leekwars.socket;

import java.util.ArrayList;

import com.leekwars.generator.scenario.EntityInfo;
import com.leekwars.generator.scenario.FarmerInfo;
import com.leekwars.generator.scenario.Scenario;
import com.leekwars.generator.scenario.TeamInfo;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class SocketScenario {
    public static Scenario fromJson(JsonNode request) {
        JsonNode scenarioNode = request.path("scenario");
        if (scenarioNode == null || !scenarioNode.isObject()) {
            throw new IllegalArgumentException("Missing or invalid field: scenario");
        }

        ObjectNode json = (ObjectNode) scenarioNode;
        Scenario scenario = new Scenario();

        if (json.has("random_seed")) {
            scenario.seed = json.get("random_seed").intValue();
        }
        if (json.has("max_turns")) {
            scenario.maxTurns = json.get("max_turns").intValue();
        }
        if (json.has("type")) {
            scenario.type = json.get("type").intValue();
        }
        if (json.has("context")) {
            scenario.context = json.get("context").intValue();
        }
        if (json.has("fight_id")) {
            scenario.fightID = json.get("fight_id").intValue();
        }
        if (json.has("boss")) {
            scenario.boss = json.get("boss").intValue();
        }
        if (json.has("draw_check_life")) {
            scenario.drawCheckLife = json.get("draw_check_life").booleanValue();
        }

        JsonNode farmersNode = json.path("farmers");
        if (farmersNode.isArray()) {
            for (JsonNode farmerJson : (ArrayNode) farmersNode) {
                if (!farmerJson.isObject()) {
                    continue;
                }
                FarmerInfo farmer = new FarmerInfo();
                farmer.id = farmerJson.path("id").intValue();
                farmer.name = farmerJson.path("name").stringValue("");
                farmer.country = farmerJson.path("country").stringValue("");
                scenario.farmers.put(farmer.id, farmer);
            }
        }

        JsonNode teamsNode = json.path("teams");
        if (teamsNode.isArray()) {
            for (JsonNode teamJson : (ArrayNode) teamsNode) {
                if (!teamJson.isObject()) {
                    continue;
                }
                TeamInfo team = new TeamInfo();
                team.id = teamJson.path("id").intValue();
                team.name = teamJson.path("name").stringValue("");
                if (teamJson.has("level")) {
                    team.level = teamJson.path("level").intValue();
                }
                if (teamJson.has("turretAI")) {
                    team.turretAI = teamJson.path("turretAI").intValue();
                }
                scenario.teams.put(team.id, team);
            }
        }

        JsonNode entitiesNode = json.path("entities");
        if (entitiesNode.isArray()) {
            for (JsonNode teamJson : (ArrayNode) entitiesNode) {
                var teamEntities = new ArrayList<EntityInfo>();
                if (teamJson.isArray()) {
                    for (JsonNode entityJson : (ArrayNode) teamJson) {
                        if (!entityJson.isObject()) {
                            continue;
                        }
                        teamEntities.add(new EntityInfo((ObjectNode) entityJson));
                    }
                }
                scenario.entities.add(teamEntities);
            }
        }

        JsonNode mapNode = json.path("map");
        if (mapNode.isObject()) {
            scenario.map = ((ObjectNode) mapNode).deepCopy();
        }

        return scenario;
    }
}
