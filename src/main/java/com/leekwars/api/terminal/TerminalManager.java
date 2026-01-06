package com.leekwars.api.terminal;

import java.io.IOException;
import java.util.List;

public class TerminalManager {
    public static ProcessBuilder processBuilder = new ProcessBuilder();

    /**
     * Execute a bash command composed of multiple tokens and return the output as a string.
     * @param bashCommandTokens List of bash command tokens
     * @param directory Directory to execute the command in (can be null)
     * @return Output of the bash command as a string, or null if an error occurred
     */
    public static String ExecuteCommand(List<String> bashCommandTokens, String directory) {
        // build bash command string separated by && to ensure each command is executed
        // in sequence

        processBuilder.command(bashCommandTokens);

        if (directory != null) {
            processBuilder.directory(new java.io.File(directory));
        }

        try {
            Process process = processBuilder.start();

            byte[] bytes = process.getInputStream().readAllBytes();
            String allText = new String(bytes);
            

            int exitVal = -1;
            try {
                exitVal = process.waitFor();
            } catch (InterruptedException e) {
                System.out.println("Bash command interrupted: " + e.getMessage());
            }
            if (exitVal == 0) {
                return allText;
            } else {
                System.out.println("Abnormal exit value: " + exitVal);
            }
            return null;

        } catch (IOException e) {
            System.out.println("IOException while executing bash command: " + e.getMessage());
        }

        return null;
    }
}
