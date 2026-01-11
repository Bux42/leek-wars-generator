package com.leekwars.api.mongo.services;

import com.leekwars.api.mongo.repositories.LeekscriptAiRepository;
import com.leekwars.pool.code.LeekscriptAI;

public class LeekScriptAiService {
    private final LeekscriptAiRepository leekscriptAis;

    public LeekScriptAiService(LeekscriptAiRepository leekscriptAis) {
        this.leekscriptAis = leekscriptAis;
    }

    public LeekscriptAI getLeekAiById(String id) {
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

    public LeekscriptAI getLeekAiByMergedAiCodeHash(String mergedAiCodeHash) {
        var docOpt = this.leekscriptAis.findByMergedAiCodeHash(mergedAiCodeHash);
        if (docOpt.isEmpty()) {
            return null;
        }

        var doc = docOpt.get();
        // Convert MongoDB document to LeekSnapshotAI instance
        String docJson = doc.toJson();
        var aiJson = com.alibaba.fastjson.JSON.parseObject(docJson);

        return LeekscriptAI.fromJson(aiJson);
    }

    public String addLeekAi(LeekscriptAI leekAi) {
        // Convert LeekSnapshotAI instance to MongoDB document
        String aiJson = com.alibaba.fastjson.JSON.toJSONString(leekAi);
        var aiData = org.bson.Document.parse(aiJson);

        // Insert document into MongoDB
        this.leekscriptAis.insert(aiData);

        // Return the generated ID
        return aiData.getObjectId("_id").toString();
    }
}
