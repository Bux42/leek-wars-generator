package com.leekwars;

import java.io.File;
import java.io.FileNotFoundException;

import com.alibaba.fastjson.JSON;
import com.leekwars.api.HttpApi;
import com.leekwars.definitions.CodeDefinitionTester;
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
					case "test_definitions":
						new CodeDefinitionTester();
						return;
				}
				if (arg.startsWith("--farmer=")) {
					farmer = Integer.parseInt(arg.substring("--farmer=".length()));
				} else if (arg.startsWith("--folder=")) {
					folder = Integer.parseInt(arg.substring("--folder=".length()));
				} else if (arg.startsWith("--download_assets")) {
					download_assets = true;
				} else if (arg.equals("--start_code_server")) {
					startApiServer(args);
					return;
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

	public static void startApiServer(String[] args) {
		int port = 8080;

		for (String a : args) {
			if (a.startsWith("--code_server_port=")) {
				try {
					port = Integer.parseInt(a.substring("--code_server_port=".length()));
				} catch (NumberFormatException e) {
					System.out.println("Invalid port number for --code_server_port");
				}
				break;
			}
		}
		final int finalPort = port;


		System.out.println("Starting code server on port " + finalPort);
		new Thread(() -> {
			try {
				HttpApi.main(new String[] { "--port=" + finalPort });
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}
}