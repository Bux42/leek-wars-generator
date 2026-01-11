package com.leekwars.pool.code;

import org.bson.json.JsonObject;

import com.alibaba.fastjson.JSONObject;

public class LeekscriptAI {
    public String id;

    public MergedCode mergedCode;
    public GitInfos gitInfos;

    public LeekscriptAI() {
    }

    public LeekscriptAI(MergedCode mergedCode, GitInfos gitInfos) {
        this.mergedCode = mergedCode;
        this.gitInfos = gitInfos;
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

        JSONObject mergedCodeObject = json.getJSONObject("mergedCode");
        MergedCode mergedCode = new MergedCode(
            mergedCodeObject.getString("hash"),
            mergedCodeObject.getString("code")
        );
        snapshot.mergedCode = mergedCode;

        JSONObject gitInfosObject = json.getJSONObject("gitInfos");
        GitInfos gitInfos = new GitInfos(
            gitInfosObject.getString("gitRepoUrl"),
            gitInfosObject.getString("gitCommitHash"),
            gitInfosObject.getBooleanValue("hasUncommittedChanges"),
            gitInfosObject.getString("gitDiffOutput")
        );
        snapshot.gitInfos = gitInfos;

        return snapshot;
    }
}
