package com.leekwars.generator.profiler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import leekscript.runner.OperationsProfiler;

public final class FlameGraphProfiler implements OperationsProfiler {

	private static final class Frame {
		private final String name;
		private String file;
		private int line;

		private Frame(String name, String file, int line) {
			this.name = name;
			this.file = file;
			this.line = line;
		}

		private String label() {
			return clean(name) + " (" + clean(file) + ":" + line + ")";
		}
	}

	private final Path outputFile;
	private final List<Frame> frames = new ArrayList<>();
	private final Map<String, Long> stacks = new HashMap<>();
	private List<String> lastStack = List.of();
	private boolean written = false;

	public FlameGraphProfiler(Path outputFile) {
		this.outputFile = outputFile;
	}

	@Override
	public void enter(String name, String file, int line) {
		if (!written) {
			frames.add(new Frame(name, file, line));
		}
	}

	@Override
	public void exit() {
		if (!written && !frames.isEmpty()) {
			frames.remove(frames.size() - 1);
		}
	}

	@Override
	public void location(String file, int line) {
		if (!written && !frames.isEmpty()) {
			var frame = frames.get(frames.size() - 1);
			frame.file = file;
			frame.line = line;
		}
	}

	@Override
	public void record(long operations) {
		if (written || operations <= 0) {
			return;
		}

		List<String> stack = currentStack();
		if (stack.isEmpty()) {
			return;
		}
		lastStack = stack;
		add(stack, operations);
	}

	private List<String> currentStack() {
		if (!frames.isEmpty()) {
			var stack = new ArrayList<String>(frames.size());
			for (var frame : frames) {
				stack.add(frame.label());
			}
			return stack;
		}
		return lastStack;
	}

	private void add(List<String> stack, long operations) {
		var key = String.join(";", stack);
		stacks.merge(key, operations, Long::sum);
	}

	public void write() {
		if (written) {
			return;
		}
		written = true;
		frames.clear();

		try {
			var parent = outputFile.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			var sortedStacks = new TreeMap<>(stacks);
			var content = new StringBuilder();
			for (var entry : sortedStacks.entrySet()) {
				content.append(entry.getKey()).append(' ').append(entry.getValue()).append('\n');
			}
			Files.writeString(outputFile, content.toString(), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
		} catch (IOException ignored) {
			// Profiling must not change fight execution.
		}
	}

	private static String clean(String value) {
		if (value == null || value.isEmpty()) {
			return "?";
		}
		return value.replace(';', '_').replace('\r', '_').replace('\n', '_');
	}
}
