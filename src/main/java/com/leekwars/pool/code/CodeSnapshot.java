package com.leekwars.pool.code;

import com.alibaba.fastjson.JSONObject;

public class CodeSnapshot {
    public String id;

    public MergedCode mergedCode;
    public GitInfos gitInfos;

    public CodeSnapshot() {
    }

    public CodeSnapshot(MergedCode mergedCode, GitInfos gitInfos) {
        this.mergedCode = mergedCode;
        this.gitInfos = gitInfos;
    }

    public static CodeSnapshot fromJson(JSONObject json) {
        CodeSnapshot snapshot = new CodeSnapshot();

        // If json comes from MongoDB, _id is an object with $oid field
        if (json.containsKey("_id")) {
            snapshot.id = json.getJSONObject("_id").getString("$oid").toString();
        }

        // else, it's from the frontend API, so id is a string field
        else if (json.containsKey("id")) {
            snapshot.id = json.getString("id");
        }

        MergedCode mergedCode = new MergedCode(
            json.getString("mergedCodeHash"),
            json.getString("mergedCode")
        );
        snapshot.mergedCode = mergedCode;

        GitInfos gitInfos = new GitInfos(
            json.getString("gitRepoUrl"),
            json.getString("gitCommitHash"),
            json.getBooleanValue("hasUncommittedChanges"),
            json.getString("gitDiffOutput")
        );
        snapshot.gitInfos = gitInfos;

        return snapshot;
    }
}
