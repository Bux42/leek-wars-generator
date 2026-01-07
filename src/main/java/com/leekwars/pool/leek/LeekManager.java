package com.leekwars.pool.leek;

import com.leekwars.api.files.FileManager;
import com.leekwars.api.git.GitManager;
import com.leekwars.generator.Generator;
import com.leekwars.pool.code.GitInfos;
import com.leekwars.pool.code.MergedCode;

import leekscript.compiler.AIFile;
import leekscript.compiler.LeekScript;

public class LeekManager {
    public static GitInfos TryGetGitInfos(Leek leek) {
        String gitCommitHash = GitManager.tryGetGitCommitHash(leek.aiFilePath);

        if (gitCommitHash == null) {
            System.out.println("LeekManager: No git repository found for leek ID " + leek.id + ", AI path: " + leek.aiFilePath);
            return null;
        } else {
            String gitRepoUrl = GitManager.tryGetGitRepoUrl(leek.aiFilePath);

            String gitDiff = GitManager.tryGetGitDiff(leek.aiFilePath);
            boolean hasUncommittedChanges = gitDiff != null && !gitDiff.isEmpty();
            if (hasUncommittedChanges) {
                System.out.println("LeekManager: Git diff for leek ID " + leek.id + ":\n" + gitDiff);
            }

            System.out.println("LeekManager: Found git repository for leek ID " + leek.id + ", commit hash: " + gitCommitHash);

            return new GitInfos(gitRepoUrl, gitCommitHash, hasUncommittedChanges, gitDiff);
        }
    }

    public static MergedCode GetLeekScriptMergedCode(Leek leek, AIFile aiFile, Generator generator) {
        // get merged AI full code
        String mergedAICode = generator.downloadAI(aiFile);

        if (mergedAICode == null) {
            System.out.println("LeekManager: Failed to get merged AI code for leek ID " + leek.id + ", AI path: " + leek.aiFilePath);
            return null;
        }

        return new MergedCode(Integer.toHexString(mergedAICode.hashCode()), mergedAICode);
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
