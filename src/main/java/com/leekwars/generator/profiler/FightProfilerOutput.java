package com.leekwars.generator.profiler;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.leekwars.generator.state.Entity;

public final class FightProfilerOutput {

	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
	private static final String INVALID_FILENAME_CHARACTERS = "<>:\"/\\|?*";

	private final Path fightDirectory;

	private FightProfilerOutput(Path fightDirectory) {
		this.fightDirectory = fightDirectory;
	}

	public static FightProfilerOutput create() {
		var root = Path.of("profiler-output").toAbsolutePath().normalize();
		try {
			Files.createDirectories(root);
			var timestamp = TIMESTAMP.format(LocalDateTime.now());
			for (int suffix = 0; suffix < 1000; suffix++) {
				var name = suffix == 0 ? timestamp : timestamp + "-" + suffix;
				var directory = root.resolve(name);
				try {
					Files.createDirectory(directory);
					return new FightProfilerOutput(directory);
				} catch (FileAlreadyExistsException ignored) {
					// Same millisecond: use next suffix.
				}
			}
		} catch (IOException ignored) {
			// Profiling must not change fight execution.
		}
		return new FightProfilerOutput(null);
	}

	public FlameGraphProfiler start(Entity entity, int turn) {
		if (fightDirectory == null) {
			return null;
		}
		try {
			var turnDirectory = fightDirectory.resolve(Integer.toString(turn));
			Files.createDirectories(turnDirectory);
			var fileName = sanitize(entity.getName()) + "_" + entity.getId() + ".folded";
			return new FlameGraphProfiler(turnDirectory.resolve(fileName));
		} catch (IOException ignored) {
			return null;
		}
	}

	private static String sanitize(String name) {
		if (name == null || name.isEmpty()) {
			return "entity";
		}
		var result = new StringBuilder(name.length());
		for (int i = 0; i < name.length(); i++) {
			char character = name.charAt(i);
			if (character < 32 || INVALID_FILENAME_CHARACTERS.indexOf(character) >= 0) {
				result.append('_');
			} else {
				result.append(character);
			}
		}
		return result.isEmpty() ? "entity" : result.toString();
	}
}
