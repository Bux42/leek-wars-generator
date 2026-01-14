package com.leekwars.api.mongo.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.api.mongo.repositories.PoolFightDuelRepository;
import com.leekwars.pool.elo.EloManager;
import com.leekwars.pool.leek.Leek;
import com.leekwars.pool.leek.PoolRunLeek;
import com.leekwars.pool.run.fight.categories.PoolFightDuel;
import com.leekwars.pool.run.fight.talent.TalentDataPoint;

public class PoolFightDuelService {
    private final PoolFightDuelRepository poolFightDuel;

    public PoolFightDuelService(PoolFightDuelRepository poolFightDuel) {
        this.poolFightDuel = poolFightDuel;
    }

    public String addPoolFight(PoolFightDuel poolFight) {
        // Convert PoolDuel instance to MongoDB Document
        String fightJson = com.alibaba.fastjson.JSON.toJSONString(poolFight);
        var fightData = org.bson.Document.parse(fightJson);

        // remove id field to avoid conflicts with MongoDB _id
        fightData.remove("id");

        // Insert pool duel into database
        this.poolFightDuel.insert(fightData);
        return fightData.getObjectId("_id").toHexString();
    }

    public int countAllPoolFights() {
        return this.poolFightDuel.countAll();
    }

    public int countAllPoolFightsByPoolRunId(String poolRunId) {
        return this.poolFightDuel.countAllByPoolRunId(poolRunId);
    }

    public List<PoolFightDuel> getAllByPoolRunId(String poolRunId) {
        var docs = this.poolFightDuel.getAllByPoolRunId(poolRunId);
        if (docs == null) {
            return null;
        }

        List<PoolFightDuel> poolFightsDuel = new ArrayList<>();

        for (var doc : docs) {
            String duelJson = doc.toJson();
            poolFightsDuel.add(PoolFightDuel.fromJson(JSON.parseObject(duelJson, JSONObject.class)));
        }

        return poolFightsDuel;
    }

    public List<TalentDataPoint> getEloProgressionByPoolRunId(String poolRunId, List<PoolRunLeek> leeks) {
        List<PoolFightDuel> fights = getAllByPoolRunId(poolRunId);
        if (fights == null) {
            return null;
        }

        List<TalentDataPoint> talentDataPoints = new ArrayList<>();

        Map<String, Integer> eloMap = new HashMap<>();

        for (var fight : fights) {
            if (!eloMap.containsKey(fight.leek1Id)) {
                eloMap.put(fight.leek1Id, 100);
            }
            if (!eloMap.containsKey(fight.leek2Id)) {
                eloMap.put(fight.leek2Id, 100);
            }

            int leek1Elo = eloMap.get(fight.leek1Id);
            int leek2Elo = eloMap.get(fight.leek2Id);

            float eloDeltaLeek1 = 0;
            float eloDeltaLeek2 = 0;

            if (fight.winnerLeekId.equals(fight.leek1Id)) {
                eloDeltaLeek1 = EloManager.GetRatingDelta(leek1Elo, leek2Elo, 1.0f);
                eloDeltaLeek2 = EloManager.GetRatingDelta(leek2Elo, leek1Elo, 0.0f);
            } else if (fight.winnerLeekId.equals(fight.leek2Id)) {
                eloDeltaLeek1 = EloManager.GetRatingDelta(leek1Elo, leek2Elo, 0.0f);
                eloDeltaLeek2 = EloManager.GetRatingDelta(leek2Elo, leek1Elo, 1.0f);
            } else {
                eloDeltaLeek1 = EloManager.GetRatingDelta(leek1Elo, leek2Elo, 0.5f);
                eloDeltaLeek2 = EloManager.GetRatingDelta(leek2Elo, leek1Elo, 0.5f);
            }

            // Update ELOs
            leek1Elo += eloDeltaLeek1;
            leek2Elo += eloDeltaLeek2;

            eloMap.put(fight.leek1Id, leek1Elo);
            eloMap.put(fight.leek2Id, leek2Elo);

            for (Leek leek : leeks) {
                if (!eloMap.containsKey(leek.id)) {
                    eloMap.put(leek.id, 100);
                }

                talentDataPoints.add(new TalentDataPoint(fight.date, leek.name, eloMap.get(leek.id)));
            }
        }

        return talentDataPoints;
    }
}
