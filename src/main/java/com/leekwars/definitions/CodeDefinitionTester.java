package com.leekwars.definitions;

import java.util.ArrayList;

import com.alibaba.fastjson.JSON;
import com.leekwars.generator.Generator;

import leekscript.compiler.LeekScript;
import leekscript.compiler.resolver.NativeFileSystem;
import leekscript.compiler.vscode.DefinitionsResult;
import leekscript.compiler.vscode.UserArgumentDefinition;
import leekscript.compiler.vscode.UserVariableDeclaration;

public class CodeDefinitionTester {
    private Generator generator;
    public ArrayList<CodeDefinitionTest> tests = new ArrayList<CodeDefinitionTest>();

    public CodeDefinitionTester() {
        LeekScript.setFileSystem(new NativeFileSystem());
        Generator generator = new Generator();
        generator.setCache(false);
        this.generator = generator;

        String rootPath = "src\\main\\java\\com\\leekwars\\definitions\\leekscript_tests\\";

        /* VARIABLES */

        var singleIntegerVariable = new DefinitionsResult();
        singleIntegerVariable.variables.add(new UserVariableDeclaration(2, 8,
                        "declare_integer_variable.leek",
                        rootPath + "variables", "integerVariable", "integer"));

        tests.add(new CodeDefinitionTest("Integer variable should be defined", 3, 1,
                        rootPath + "variables/declare_integer_variable.leek",
                        singleIntegerVariable));
        
        // CodeDefinitionTest firstTest = tests.get(0);

        // System.out.println(firstTest.toString());
        // System.out.println(singleIntegerVariable.variables.get(0).toString());

        var emptyDefinitionResult = new DefinitionsResult();
        tests.add(new CodeDefinitionTest("No variable should be defined 1", 1, 1,
                        rootPath + "variables/declare_integer_variable.leek",
                        emptyDefinitionResult));

        tests.add(new CodeDefinitionTest("No variable should be defined 2", 2, 1,
                        rootPath + "variables/declare_integer_variable.leek",
                        emptyDefinitionResult));

        var singleAnyVariable = new DefinitionsResult();
        singleAnyVariable.variables.add(new UserVariableDeclaration(2, 4,
                        "declare_any_variable.leek",
                        rootPath + "variables", "anyVariable", "any"));
        tests.add(new CodeDefinitionTest("Any variable should be defined", 3, 1,
                        rootPath + "variables/declare_any_variable.leek",
                        singleAnyVariable));

        /* CLASSES */
        var emptyClassesResult = new DefinitionsResult();
        emptyClassesResult.classes.add(new leekscript.compiler.vscode.UserClassDefinition(2, 6,
                        "declare_class.leek",
                        rootPath + "classes", "Test", null));
        tests.add(new CodeDefinitionTest("Empty class should be defined", 1, 1,
                        rootPath + "classes/declare_class.leek", emptyClassesResult));

        var emptyClassEmptyConstructorResult = new DefinitionsResult();
        var classDef = new leekscript.compiler.vscode.UserClassDefinition(2, 6,
                        "declare_class_with_empty_constructor.leek",
                        rootPath + "classes", "Test", null);
        var constructorDef = new leekscript.compiler.vscode.UserClassMethodDefinition(3, 4,
                        "declare_class_with_empty_constructor.leek",
                        rootPath + "classes", "constructor", "void");
        classDef.addConstructor(constructorDef);
        emptyClassEmptyConstructorResult.classes.add(classDef);
        tests.add(new CodeDefinitionTest("Empty class with empty constructor should be defined", 4, 1,
                        rootPath + "classes/declare_class_with_empty_constructor.leek",
                        emptyClassEmptyConstructorResult));

        var emptyClassConstructorWithArgsResult = new DefinitionsResult();
        var classDef2 = new leekscript.compiler.vscode.UserClassDefinition(2, 6,
                        "declare_class_with_constructor_with_args.leek",
                        rootPath + "classes", "Test", null);
        var constructorDef2 = new leekscript.compiler.vscode.UserClassMethodDefinition(3, 4,
                        "declare_class_with_constructor_with_args.leek",
                        rootPath + "classes", "constructor", "void");
        UserArgumentDefinition argDef = new UserArgumentDefinition("nb", "integer");

        constructorDef2.addArgument("", false, argDef);
        classDef2.addConstructor(constructorDef2);
        emptyClassConstructorWithArgsResult.classes.add(classDef2);

        // the cursor is inside the class, so the `this` variable should be defined
        emptyClassConstructorWithArgsResult.variables.add(new UserVariableDeclaration(2, 6,
                        "declare_class_with_constructor_with_args.leek",
                        rootPath + "classes", "this", "Test"));
        // the cursor in inside the constructor, so the variable nb should also be
        // defined
        emptyClassConstructorWithArgsResult.variables.add(new UserVariableDeclaration(3, 24,
                        "declare_class_with_constructor_with_args.leek",
                        rootPath + "classes", "nb", "integer"));

        tests.add(new CodeDefinitionTest(
                        "Empty class with constructor with args should be defined, as well as the nb param variable and `this` variable",
                        4, 1,
                        rootPath + "classes/declare_class_with_constructor_with_args.leek",
                        emptyClassConstructorWithArgsResult));
        // leekscript.compiler.UserArgumentDefinition();
        runTests();
    }

    /**
     * Prints the code with a cursor indicator at the specified line and column.
     * 
     * @param code
     * @param line
     * @param column
     */
    private void printCodeWithCusor(String code, int line, int column) {
        boolean found = false;
        System.out.println("___CODE_START___");
        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (i == line - 1) {
                    System.out.print("> ");
                    System.out.println(lines[i]);
                    found = true;
            } else {
                    System.out.println(lines[i]);
            }
        }
        if (!found) {
            System.out.println("> ");
        }
        System.out.println("___CODE_END___");
    }

    private void printCodeDiff(String expected, String actual) {
        String[] lines = expected.split("\n");
        String[] actualLines = actual.split("\n");

        int maxLines = Math.max(lines.length, actualLines.length);

        for (int i = 0; i < maxLines; i++) {
            String expectedLine = i < lines.length ? lines[i] : "<no line>";
            String actualLine = i < actualLines.length ? actualLines[i] : "<no line>";

            if (!expectedLine.equals(actualLine)) {
                    System.out.println("Expected: " + expectedLine);
                    System.out.println("Actual  : " + actualLine);
            }
        }
    }

    public void runTests() {
        boolean debugDefinitionsCompiler = false;

        int passed = 0;
        int failed = 0;

        for (var test : tests) {
            System.out.println("Running test: [" + test.testName + "] for file: " + test.fileLocation
                            + " at line: "
                            + test.cursorLine
                            + " column: " + test.cursorColumn);
            try {
                var ai = LeekScript.getFileSystem().getRoot().resolve(test.fileLocation);

                String aiCode = ai.getCode();
                printCodeWithCusor(aiCode, test.cursorLine, test.cursorColumn);

                DefinitionsResult actualResult = generator.getDefinitions(ai,
                                test.cursorLine, test.cursorColumn, debugDefinitionsCompiler);
                // store json result in string
                String actualResultJsonString = JSON.toJSONString(actualResult, true);
                String expectedResultJsonString = JSON.toJSONString(test.expectedResult, true);

                if (actualResultJsonString.equals(expectedResultJsonString)) {
                        System.out.println("Test passed.");
                        passed++;
                } else {
                        System.out
                                        .println("Test failed.\nExpected: " + expectedResultJsonString
                                                        + "\nActual: "
                                                        + actualResultJsonString);
                        printCodeDiff(expectedResultJsonString, actualResultJsonString);
                        failed++;
                }

            } catch (Exception ex) {
                System.out.println("Exception during test: " + ex.getMessage());
                ex.printStackTrace();
                failed++;
            }
        }
        System.out.println("Tests completed. Passed: " + passed + ", Failed: " + failed);
    }
}
