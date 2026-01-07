package com.leekwars.api.mongo.services;

import com.leekwars.api.mongo.repositories.LeekAiRepository;
import com.leekwars.pool.code.CodeSnapshot;

public class LeekScriptAiService {
    private final LeekAiRepository leekAis;

    public LeekScriptAiService(LeekAiRepository leekAis) {
        this.leekAis = leekAis;
    }

    public CodeSnapshot getLeekAiById(String id) {
        var docOpt = this.leekAis.findById(id);
        if (docOpt.isEmpty()) {
            return null;
        }

        var doc = docOpt.get();
        // Convert MongoDB document to LeekSnapshotAI instance
        String docJson = doc.toJson();
        var aiJson = com.alibaba.fastjson.JSON.parseObject(docJson);

        return CodeSnapshot.fromJson(aiJson);
    }

    public CodeSnapshot getLeekAiByMergedAiCodeHash(String mergedAiCodeHash) {
        var docOpt = this.leekAis.findByMergedAiCodeHash(mergedAiCodeHash);
        if (docOpt.isEmpty()) {
            return null;
        }

        var doc = docOpt.get();
        // Convert MongoDB document to LeekSnapshotAI instance
        String docJson = doc.toJson();
        var aiJson = com.alibaba.fastjson.JSON.parseObject(docJson);

        return CodeSnapshot.fromJson(aiJson);
    }

    public String addLeekAi(CodeSnapshot leekAi) {
        // Convert LeekSnapshotAI instance to MongoDB document
        String aiJson = com.alibaba.fastjson.JSON.toJSONString(leekAi);
        var aiData = org.bson.Document.parse(aiJson);

        // Insert document into MongoDB
        this.leekAis.insert(aiData);

        // Return the generated ID
        return aiData.getObjectId("_id").toString();
    }
}
