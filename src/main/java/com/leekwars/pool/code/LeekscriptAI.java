package com.leekwars.pool.code;

import com.alibaba.fastjson.JSONObject;

public class LeekscriptAI {
    public String id;

    public String name;
    public String description;
    public String originalFilePath;
    public long creationDate;

    public MergedCode mergedCode;
    public GitInfos gitInfos;

    public LeekscriptAI() {
    }

    public LeekscriptAI(MergedCode mergedCode, GitInfos gitInfos) {
        this.mergedCode = mergedCode;
        this.gitInfos = gitInfos;
    }

    public LeekscriptAI(MergedCode mergedCode, GitInfos gitInfos, String name, String description, String originalFilePath) {
        this.mergedCode = mergedCode;
        this.gitInfos = gitInfos;
        this.name = name;
        this.description = description;
        this.originalFilePath = originalFilePath;
        this.creationDate = System.currentTimeMillis();
    }

    public static LeekscriptAI fromJson(JSONObject json) {
        LeekscriptAI snapshot = new LeekscriptAI();

        // If json comes from MongoDB, _id is an object with $oid field
        if (json.containsKey("_id")) {
            snapshot.id = json.getJSONObject("_id").getString("$oid").toString();
        }

        // else, it's from the frontend API, so id is a string field
        else if (json.containsKey("id")) {
            snapshot.id = json.getString("id");
        }

        snapshot.name = json.getString("name");
        snapshot.description = json.getString("description");
        snapshot.originalFilePath = json.getString("originalFilePath");
        snapshot.creationDate = json.getLongValue("creationDate");

        JSONObject mergedCodeObject = json.getJSONObject("mergedCode");
        MergedCode mergedCode = new MergedCode(
            mergedCodeObject.getString("hash"),
            mergedCodeObject.getString("code")
        );
        snapshot.mergedCode = mergedCode;

        JSONObject gitInfosObject = json.getJSONObject("gitInfos");
        GitInfos gitInfos = new GitInfos(
            gitInfosObject.getString("repoUrl"),
            gitInfosObject.getString("branchName"),
            gitInfosObject.getString("commitHash"),
            gitInfosObject.getBooleanValue("hasUncommittedChanges"),
            gitInfosObject.getString("diffOutput")
        );
        snapshot.gitInfos = gitInfos;

        return snapshot;
    }
}
