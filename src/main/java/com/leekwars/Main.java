package com.leekwars;

import java.io.File;
import java.io.FileNotFoundException;

import com.alibaba.fastjson.JSON;
import com.leekwars.api.HttpApi;
import com.leekwars.generator.Data;
import com.leekwars.generator.Generator;
import com.leekwars.generator.Log;
import com.leekwars.generator.outcome.Outcome;
import com.leekwars.generator.scenario.Scenario;
import com.leekwars.generator.test.LocalDbFileSystem;
import com.leekwars.generator.test.LocalDbRegisterManager;
import com.leekwars.generator.test.LocalTrophyManager;

import leekscript.compiler.LeekScript;
import leekscript.compiler.IACompiler.AnalyzeResult;
import leekscript.compiler.resolver.NativeFileSystem;

public class Main {

	private static final String TAG = Main.class.getSimpleName();

	public static void main(String[] args) {
		String file = null;
		boolean nocache = false;
		boolean db_resolver = false;
		boolean verbose = false;
		boolean analyze = false;
		int farmer = 0;
		int folder = 0;

		// set to true to download remote leekwars assets json files (needed for the generator)
		boolean download_assets = false;

		for (String arg : args) {
			if (arg.startsWith("--")) {
				switch (arg.substring(2)) {
					case "nocache": nocache = true; break;
					case "dbresolver": db_resolver = true; break;
					case "verbose": verbose = true; break;
					case "analyze": analyze = true; break;
				}
				if (arg.startsWith("--farmer=")) {
					farmer = Integer.parseInt(arg.substring("--farmer=".length()));
				} else if (arg.startsWith("--folder=")) {
					folder = Integer.parseInt(arg.substring("--folder=".length()));
				} else if (arg.startsWith("--download_assets")) {
					download_assets = true;
				} else if (arg.startsWith("--start_code_server")) {
					// Start a simple HTTP server to serve code files
					// Check if we should also start tests
					boolean startTests = false;
					for (String a : args) {
						if ("--start_tests".equals(a)) {
							startTests = true;
							break;
						}
					}
					final boolean runTests = startTests;
					new Thread(() -> {
						try {
							HttpApi.main(runTests ? new String[] { "--start_tests" } : new String[] {});
						} catch (Exception e) {
							e.printStackTrace();
						}
					}).start();
				} 
			} else {
				file = arg;
			}
		}
		Log.enable(verbose);
		System.out.println("Generator v1");

		if (download_assets) {
			Data.checkData("https://leekwars.com/api/");
		}

		// System.out.println("db_resolver " + db_resolver + " folder=" + folder + " farmer=" + farmer);
		if (file == null) {
			System.out.println("No scenario/ai file passed!");
			return;
		}

		// Data.checkData("https://leekwars.com/api/");
		if (db_resolver) {
			LeekScript.setFileSystem(new LocalDbFileSystem());
		} else {
			LeekScript.setFileSystem(new NativeFileSystem());
		}
		Generator generator = new Generator();
		generator.setCache(!nocache);
		if (analyze) {
			try {
				var ai = LeekScript.getFileSystem().getRoot().resolve(file);
				AnalyzeResult result = generator.analyzeAI(ai, 0);
				System.out.println(result.informations);
			} catch (FileNotFoundException e) {
				Log.e(TAG, "File not found!");
			}
		} else {
			Scenario scenario = Scenario.fromFile(new File(file));
			if (scenario == null) {
				Log.e(TAG, "Failed to parse scenario!");
				return;
			}
			Outcome outcome = generator.runScenario(scenario, null, new LocalDbRegisterManager(), new LocalTrophyManager());
			System.out.println(JSON.toJSONString(outcome.toJson(), false));
		}
	}
}