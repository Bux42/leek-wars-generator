package com.leekwars.pool.leek;

import com.leekwars.api.files.FileManager;
import com.leekwars.api.git.GitManager;
import com.leekwars.generator.Generator;
import com.leekwars.pool.code.GitInfos;
import com.leekwars.pool.code.MergedCode;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

public class LeekManager {
    public static GitInfos TryGetGitInfos(String aiFilePath) {
        String gitCommitHash = GitManager.tryGetGitCommitHash(aiFilePath);

        if (gitCommitHash == null) {
            System.out.println("LeekManager: No git repository found for AI path: " + aiFilePath);
            return null;
        } else {
            String gitRepoUrl = GitManager.tryGetGitRepoUrl(aiFilePath);
            String gitBranchName = GitManager.tryGetGitBranchName(aiFilePath);
            String gitDiff = GitManager.tryGetGitDiff(aiFilePath);
            boolean hasUncommittedChanges = gitDiff != null && !gitDiff.isEmpty();
            if (hasUncommittedChanges) {
                System.out.println("LeekManager: Git diff for AI path " + aiFilePath + ":\n" + gitDiff);
            }

            System.out.println("LeekManager: Found git repository for AI path " + aiFilePath + ", commit hash: " + gitCommitHash);

            return new GitInfos(gitRepoUrl, gitBranchName, gitCommitHash, hasUncommittedChanges, gitDiff);
        }
    }

    public static MergedCode GetLeekScriptMergedCode(AIFile aiFile, Generator generator) {
        // get merged AI full code
        generator.setCache(false);
        String mergedAICode = generator.downloadAI(aiFile);
        generator.setCache(true);

        if (mergedAICode == null) {
            System.out.println("LeekManager: Failed to get merged AI code for AI path: " + aiFile.getFolder());
            return null;
        }

        return new MergedCode(Integer.toHexString(mergedAICode.hashCode()), mergedAICode);
    }

    public static String GetAiMergedCode(AIFile aiFile, PoolRunLeek poolRunLeek, Generator generator) {
        String mergedAICode = generator.downloadAI(aiFile);
        return mergedAICode;
    }

    public static AIFile ResolveAIFile(String aiFilePath, Generator generator) {
        String relativeAiFilePath = GetSanitizedRelativeAiFilePath(aiFilePath);

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
        String sanitizedAiFilePath = FileManager.sanitizeAbsoluteFilePath(poolRunLeek.mergedCodeHash);
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
