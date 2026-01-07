package com.leekwars.pool.code;

public class GitInfos {
    public String gitRepoUrl;
    public String gitCommitHash;
    public boolean hasUncommittedChanges;
    public String gitDiffOutput;

    public GitInfos(String gitRepoUrl, String gitCommitHash, boolean hasUncommittedChanges, String gitDiffOutput) {
        this.gitRepoUrl = gitRepoUrl;
        this.gitCommitHash = gitCommitHash;
        this.hasUncommittedChanges = hasUncommittedChanges;
        this.gitDiffOutput = gitDiffOutput;
    }
}
