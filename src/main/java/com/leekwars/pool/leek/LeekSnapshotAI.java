package com.leekwars.pool.leek;

import com.alibaba.fastjson.JSONObject;

public class LeekSnapshotAI {
    public String mergedAiCodeHash;
    public String mergedAiCode;

    // only set if the AI is stored in a git repository
    public boolean hasGitRepo;
    public String gitRepoUrl;
    public String gitCommitHash;
    public boolean hasUncommittedChanges;
    public String gitDiffOutput;

    public static LeekSnapshotAI fromJson(JSONObject json) {
        LeekSnapshotAI snapshot = new LeekSnapshotAI();
        snapshot.mergedAiCodeHash = json.getString("mergedAiCodeHash");
        snapshot.mergedAiCode = json.getString("mergedAiCode");
        snapshot.hasGitRepo = json.getBooleanValue("hasGitRepo");
        snapshot.gitRepoUrl = json.getString("gitRepoUrl");
        snapshot.gitCommitHash = json.getString("gitCommitHash");
        snapshot.hasUncommittedChanges = json.getBooleanValue("hasUncommittedChanges");
        snapshot.gitDiffOutput = json.getString("gitDiffOutput");
        return snapshot;
    }
}
