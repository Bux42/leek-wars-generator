package com.leekwars.api.mongo.services;

import java.util.List;

import com.leekwars.api.mongo.repositories.LeekRepository;
import com.leekwars.pool.leek.Leek;

public class LeekService {
    private final LeekRepository leeks;

    public LeekService(LeekRepository leeks) {
        this.leeks = leeks;
    }

    public Leek getLeekById(String id) {
        var docOpt = this.leeks.findById(id);
        if (docOpt.isEmpty()) {
            return null;
        }

        var doc = docOpt.get();
        // Convert MongoDB document to Leek instance
        String docJson = doc.toJson();
        var leekJson = com.alibaba.fastjson.JSON.parseObject(docJson);

        return Leek.fromJson(leekJson);
    }

    public List<Leek> getLeeksByIds(List<String> ids) {
        var docs = this.leeks.findByIds(ids);
        List<Leek> leeksList =  new java.util.ArrayList<>();

        for (var doc : docs) {
            // Convert MongoDB document to Leek instance
            String docJson = doc.toJson();
            var leekJson = com.alibaba.fastjson.JSON.parseObject(docJson);

            Leek leek = Leek.fromJson(leekJson);

            leeksList.add(leek);
        }

        return leeksList;
    }

    public List<Leek> getAllLeeks() {
        List<Leek> allLeeks =  new java.util.ArrayList<>();

        for (var doc : this.leeks.findAll()) {
            // Convert MongoDB document to Leek instance
            String docJson = doc.toJson();
            var leekJson = com.alibaba.fastjson.JSON.parseObject(docJson);

            Leek leek = Leek.fromJson(leekJson);

            allLeeks.add(leek);
        }

        return allLeeks;
    }

    public String addLeek(Leek leek) {
        // Convert Leek instance to MongoDB Document
        String leekJson = com.alibaba.fastjson.JSON.toJSONString(leek);
        var leekData = org.bson.Document.parse(leekJson);

        // Insert leek into database
        this.leeks.insert(leekData);

        // Return the ID of the newly created leek
        return leekData.getObjectId("_id").toHexString();
    }

    public boolean deleteLeek(String id) {
        return this.leeks.delete(id);
    }

    public boolean updateLeek(Leek leek) {
        // Convert Leek instance to MongoDB Document
        String leekJson = com.alibaba.fastjson.JSON.toJSONString(leek);
        var leekData = org.bson.Document.parse(leekJson);

        String leekId = leek.id;

        // remove id from document as it's used as _id in MongoDB
        leekData.remove("id");

        // Update leek in database
        return this.leeks.update(leekData, leekId);
    }
}
