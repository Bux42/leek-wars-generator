package com.leekwars;

import java.io.File;
import java.io.FileNotFoundException;

import com.alibaba.fastjson.JSON;
import com.leekwars.generator.Generator;
import com.leekwars.generator.Log;
import com.leekwars.generator.Data;
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
		int run_count = 1;
		int farmer = 0;
		int folder = 0;

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
				} else if (arg.startsWith("--run_count=")) {
					run_count = Integer.parseInt(arg.substring("--run_count=".length()));
					System.out.println("run_count: " + run_count);
				}
			} else {
				file = arg;
			}
		}
		Log.enable(verbose);
		Log.i(TAG, "Generator v1");
		// System.out.println("db_resolver " + db_resolver + " folder=" + folder + " farmer=" + farmer);
		if (file == null) {
			Log.i(TAG, "No scenario/ai file passed!");
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
			if (run_count > 1) {
				int player_0_win = 0;
				int player_1_win = 0;
				int draw = 0;

				for (int i = 0; i < run_count; i++) {
					Scenario scenario = Scenario.fromFile(new File(file));
					if (scenario == null) {
						Log.e(TAG, "Failed to parse scenario!");
						return;
					}
					Outcome outcome = generator.runScenario(scenario, null, new LocalDbRegisterManager(), new LocalTrophyManager());
					// System.out.println(JSON.toJSONString(outcome.toJson(), false));
					// System.out.println(outcome.winner);
					if (outcome.winner == 0) {
						player_0_win++;
					} else if (outcome.winner == 1) {
						player_1_win++;
					} else {
						draw++;
					}
				}
				System.out.println("Player 0 win: " + player_0_win);
				System.out.println("Player 1 win: " + player_1_win);
				System.out.println("Draw: " + draw);
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
}