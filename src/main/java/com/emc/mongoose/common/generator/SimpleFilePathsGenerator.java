package com.emc.mongoose.common.generator;

import java.util.Random;

public class SimpleFilePathsGenerator implements ValueGenerator<String> {

	private static final String DIR_NAME_PREFIX = "d";
	private static final Random random = new Random();

	private SimpleFilePathsGenerator branch;
	private String path;
	private String value;

	public SimpleFilePathsGenerator(int width, int depth) {
		this(width, depth, DIR_NAME_PREFIX);
	}

	private SimpleFilePathsGenerator(int width, int depth, String value) {
		this.value = value;
		if (depth == 0) {
			return;
		}
		depth--;
		int suffix = random.nextInt(width) + 1;
		this.branch = new SimpleFilePathsGenerator(width, depth, value + suffix);
		this.path = writePath("", true);
	}

	public String getValue() {
		return value;
	}

	public String writePath(String path, boolean rootFlag) {
		if (branch == null) {
			return path;
		}
		if (!rootFlag) {
			path += value + "/";
		}
		return branch.writePath(path, false);
	}

	@Override
	public String get() {
		return path;
	}
}
