package com.emc.mongoose.common.generator;

import java.util.ArrayList;
import java.util.List;

public class SimpleFilePathsGenerator {

	private static final String DIR_NAME_PREFIX = "d";

	private List<SimpleFilePathsGenerator> branches = new ArrayList<>();
	private List<String> paths = new ArrayList<>();
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
		for (int i = 1; i < width + 1; i++) {
			branches.add(new SimpleFilePathsGenerator(width, depth, value + i));
		}
		writePaths(paths, "", true);
	}

	public String getValue() {
		return value;
	}

	private void writePaths(List<String> paths, String path, boolean rootFlag) {
		if (!rootFlag) {
			path +=  value  + "/";
		}
		if (branches.isEmpty()) {
			paths.add(path);
		}
		for (SimpleFilePathsGenerator branch: branches) {
			branch.writePaths(paths, path, false);
		}
	}

	public void printPaths() {
		for (String path: paths) {
			System.out.println(path);
		}
	}

	public List<String> paths() {
		return paths;
	}
}
