package com.leekwars.api.mongo.services;

import java.util.List;

import com.leekwars.api.mongo.repositories.LeekscriptAiRepository;
import com.leekwars.pool.code.LeekscriptAI;

public class LeekScriptAiService {
    private final LeekscriptAiRepository leekscriptAis;

    public LeekScriptAiService(LeekscriptAiRepository leekscriptAis) {
        this.leekscriptAis = leekscriptAis;
    }

    public LeekscriptAI getLeekscriptAiById(String id) {
        var docOpt = this.leekscriptAis.findById(id);
        if (docOpt.isEmpty()) {
            return null;
        }

        var doc = docOpt.get();
        // Convert MongoDB document to LeekSnapshotAI instance
        String docJson = doc.toJson();
        var aiJson = com.alibaba.fastjson.JSON.parseObject(docJson);

        return LeekscriptAI.fromJson(aiJson);
    }

    public LeekscriptAI getLeekscriptAiByMergedAiCodeHash(String mergedAiCodeHash, boolean removeCode) {
        var docOpt = this.leekscriptAis.findByMergedAiCodeHash(mergedAiCodeHash);
        if (docOpt.isEmpty()) {
            return null;
        }

        var doc = docOpt.get();
        // Convert MongoDB document to LeekSnapshotAI instance
        String docJson = doc.toJson();
        var aiJson = com.alibaba.fastjson.JSON.parseObject(docJson);

        LeekscriptAI leekAi = LeekscriptAI.fromJson(aiJson);

        if (removeCode) {
            leekAi.mergedCode.code = null;
        }

        return leekAi;
    }

    public List<LeekscriptAI> getAllLeekscriptAis(boolean removeCode) {
        var docs = this.leekscriptAis.findAll();
        List<LeekscriptAI> leekAis = new java.util.ArrayList<>();

        for (var doc : docs) {
            String docJson = doc.toJson();
            var aiJson = com.alibaba.fastjson.JSON.parseObject(docJson);
            LeekscriptAI leekAi = LeekscriptAI.fromJson(aiJson);

            if (removeCode) {
                leekAi.mergedCode.code = null;
            }

            leekAis.add(leekAi);
        }

        return leekAis;
    }

    public String addLeekscriptAi(LeekscriptAI leekAi) {
        // Convert LeekSnapshotAI instance to MongoDB document
        String aiJson = com.alibaba.fastjson.JSON.toJSONString(leekAi);
        var aiData = org.bson.Document.parse(aiJson);

        // Insert document into MongoDB
        this.leekscriptAis.insert(aiData);

        // Return the generated ID
        return aiData.getObjectId("_id").toString();
    }
}
