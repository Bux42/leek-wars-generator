package com.leekwars.pool.leek;

import com.leekwars.api.files.FileManager;
import com.leekwars.api.git.GitManager;
import com.leekwars.api.mongo.MongoDbManager;
import com.leekwars.generator.Generator;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

public class LeekManager {
    /**
     * Create a PoolRunLeek from a Leek, resolving and merging its AI code
     * information
     * 
     * @param leek
     *            The original Leek object
     * @param generator
     *            The Generator used to process AI files
     * @return PoolRunLeek
     */
    public static PoolRunLeek CreatePoolRunLeek(Leek leek, Generator generator, MongoDbManager mongoDbManager) {
        PoolRunLeek poolRunLeek = new PoolRunLeek(leek);

        LeekSnapshotAI leekSnapshotAI = new LeekSnapshotAI();

        AIFile aiFile = ResolveAIFile(leek, generator);

        if (aiFile == null) {
            System.out.println("LeekManager: AI file not found for leek ID " + leek.id + ", AI path: " + poolRunLeek.aiFilePath);
            return poolRunLeek;
        }

        // get merged AI full code
        String aiMergedCode = GetAiMergedCode(aiFile, poolRunLeek, generator);

        if (aiMergedCode == null) {
            System.out.println("LeekManager: Failed to get merged AI code for leek ID " + leek.id + ", AI path: " + poolRunLeek.aiFilePath);
            return poolRunLeek;
        }

        // compute hash of merged AI code
        String aiMergedCodeHashHex = Integer.toHexString(aiMergedCode.hashCode());

        // set snapshot info
        poolRunLeek.mergedAiCodeHash = aiMergedCodeHashHex;

        // fill snapshot object
        leekSnapshotAI.mergedAiCodeHash = aiMergedCodeHashHex;
        leekSnapshotAI.mergedAiCode = aiMergedCode;

        // check if we have an entry in db already for this hash
        LeekSnapshotAI existingSnapshot = mongoDbManager.getLeekSnapshotAI(aiMergedCodeHashHex);

        if (existingSnapshot != null) {
            System.out.println("LeekManager: Found existing LeekSnapshotAI for leek ID " + leek.id + " with hash: " + aiMergedCodeHashHex);
            return poolRunLeek;
        }

        // get git commit hash
        String gitCommitHash = GitManager.tryGetGitCommitHash(leek.aiFilePath);

        if (gitCommitHash == null) {
            leekSnapshotAI.hasGitRepo = false;
            System.out.println("LeekManager: No git repository found for leek ID " + leek.id + ", AI path: " + leek.aiFilePath);
        } else {
            leekSnapshotAI.hasGitRepo = true;
            leekSnapshotAI.gitCommitHash = gitCommitHash;

            String gitRepoUrl = GitManager.tryGetGitRepoUrl(leek.aiFilePath);
            leekSnapshotAI.gitRepoUrl = gitRepoUrl;

            String gitDiff = GitManager.tryGetGitDiff(leek.aiFilePath);
            if (gitDiff != null && !gitDiff.isEmpty()) {
                System.out.println("LeekManager: Git diff for leek ID " + leek.id + ":\n" + gitDiff);
                leekSnapshotAI.hasUncommittedChanges = true;
                leekSnapshotAI.gitDiffOutput = gitDiff;
            } else {
                leekSnapshotAI.hasUncommittedChanges = false;
            }
            System.out.println("LeekManager: Found git repository for leek ID " + leek.id + ", commit hash: " + gitCommitHash);
        }

        // store snapshot in db
        String snapshotId = mongoDbManager.addLeekSnapshotAI(leekSnapshotAI);
        System.out.println("LeekManager: Stored LeekSnapshotAI for leek ID " + leek.id + " with snapshot ID: " + snapshotId);
        return poolRunLeek;
    }

    public static String GetAiMergedCode(AIFile aiFile, PoolRunLeek poolRunLeek, Generator generator) {
        String mergedAICode = generator.downloadAI(aiFile);
        return mergedAICode;
    }

    public static AIFile ResolveAIFile(Leek leek, Generator generator) {
        String relativeAiFilePath = GetSanitizedRelativeAiFilePath(leek.aiFilePath);

        try {
            var ai = LeekScript.getFileSystem().getRoot().resolve(relativeAiFilePath);
            return ai;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String GetSanitizedAiFilePath(PoolRunLeek poolRunLeek) {
        // Sanitize / Reconstruct the AI file path
        String sanitizedAiFilePath = FileManager.sanitizeAbsoluteFilePath(poolRunLeek.aiFilePath);
        return sanitizedAiFilePath;
    }

    public static String GetRelativeAiFilePath(PoolRunLeek poolRunLeek) {
        String sanitizedAiFilePath = GetSanitizedAiFilePath(poolRunLeek);
        // Convert to relative path because the generator uses relative paths
        String relativeAiFilePath = FileManager.absolutePathToRelative(sanitizedAiFilePath);
        return relativeAiFilePath;
    }

    public static String GetSanitizedRelativeAiFilePath(String aiFilePath) {
        // Sanitize / Reconstruct the AI file path
        String sanitizedAiFilePath = FileManager.sanitizeAbsoluteFilePath(aiFilePath);
        // Convert to relative path because the generator uses relative paths
        String relativeAiFilePath = FileManager.absolutePathToRelative(sanitizedAiFilePath);

        return relativeAiFilePath;
    }
}
