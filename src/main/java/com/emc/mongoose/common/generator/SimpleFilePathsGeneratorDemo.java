package com.emc.mongoose.common.generator;


public class SimpleFilePathsGeneratorDemo {

	public static void main(String[] args) {
		SimpleFilePathsGenerator tree = new SimpleFilePathsGenerator(4, 3);
		tree.printPaths();
	}

}
