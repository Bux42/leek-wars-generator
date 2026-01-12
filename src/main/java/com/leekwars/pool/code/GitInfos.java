package com.leekwars.pool.code;

public class GitInfos {
    public String repoUrl;
    public String branchName;
    public String commitHash;
    public boolean hasUncommittedChanges;
    public String diffOutput;

    public GitInfos(String repoUrl, String branchName, String commitHash, boolean hasUncommittedChanges, String diffOutput) {
        this.repoUrl = repoUrl;
        this.branchName = branchName;
        this.commitHash = commitHash;
        this.hasUncommittedChanges = hasUncommittedChanges;
        this.diffOutput = diffOutput;
    }
}
