package com.leekwars.api.git;

import java.util.ArrayList;
import java.util.List;

import com.leekwars.api.files.FileManager;
import com.leekwars.api.terminal.TerminalManager;

public class GitManager {
    public static ProcessBuilder processBuilder = new ProcessBuilder();

    public static String tryGetGitCommitHash(String aiFilePath) {
        // get the parent directory of the AI file path
        String sanitizedAiFileDirectoryPath = FileManager.GetSanitizedRelativeAiFilePath(aiFilePath);

        List<String> terminalCommandTokens = new ArrayList<>();
        terminalCommandTokens.add("git");
        terminalCommandTokens.add("rev-parse");
        terminalCommandTokens.add("HEAD");

        try {
            String bashOutput = TerminalManager.ExecuteCommand(terminalCommandTokens, sanitizedAiFileDirectoryPath).trim();

            if (bashOutput != null && bashOutput.matches("[a-f0-9]{40}")) {
                return bashOutput;
            }
        } catch (Exception e) {
            System.out.println("Exception while getting git commit hash: " + e.getMessage());
            return null;
        }

        return null;
    }

    public static String tryGetGitDiff(String aiFilePath) {
        // get the parent directory of the AI file path
        String sanitizedAiFileDirectoryPath = FileManager.GetSanitizedRelativeAiFilePath(aiFilePath);
        System.out.println("Generating git diff for AI file path: " + sanitizedAiFileDirectoryPath);

        List<String> terminalCommandTokens = new ArrayList<>();
        terminalCommandTokens.add("git");
        terminalCommandTokens.add("diff");
        terminalCommandTokens.add("--color");

        String bashOutput = TerminalManager.ExecuteCommand(terminalCommandTokens, sanitizedAiFileDirectoryPath);

        System.out.println("Bash output:\n" + bashOutput);

        return bashOutput;
    }

    public static String tryGetGitRepoUrl(String aiFilePath) {
        // get the parent directory of the AI file path
        String sanitizedAiFileDirectoryPath = FileManager.GetSanitizedRelativeAiFilePath(aiFilePath);

        List<String> terminalCommandTokens = new ArrayList<>();
        terminalCommandTokens.add("git");
        terminalCommandTokens.add("config");
        terminalCommandTokens.add("--get");
        terminalCommandTokens.add("remote.origin.url");

        String bashOutput = TerminalManager.ExecuteCommand(terminalCommandTokens, sanitizedAiFileDirectoryPath).trim();

        if (bashOutput != null && !bashOutput.isEmpty()) {
            return bashOutput;
        }

        return null;
    }
}
