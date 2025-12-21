package com.leekwars.definitions;

import leekscript.compiler.vscode.DefinitionsResult;

public class CodeDefinitionTest {
    public String testName;
    public int cursorLine;
    public int cursorColumn;
    public String fileLocation;
    public DefinitionsResult expectedResult;

    public CodeDefinitionTest(String testName, int cursorLine, int cursorColumn, String fileLocation,
            DefinitionsResult expectedResult) {
        this.testName = testName;
        this.cursorLine = cursorLine;
        this.cursorColumn = cursorColumn;
        this.fileLocation = fileLocation;
        this.expectedResult = expectedResult;
    }

    public String toString() {
        return "Test: " + testName + " at (" + cursorLine + "," + cursorColumn + ") in file " + fileLocation;
    }
}
