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
}
