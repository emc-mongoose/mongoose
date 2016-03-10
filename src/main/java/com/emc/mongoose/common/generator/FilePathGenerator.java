package com.emc.mongoose.common.generator;

import com.emc.mongoose.common.math.MathUtil;

import java.util.Random;

import static com.emc.mongoose.common.math.MathUtil.xorShift;
import static java.lang.Integer.parseInt;
import static java.lang.Math.abs;

public class FilePathGenerator implements ValueGenerator<String> {

	public static final String DIR_NAME_PREFIX = "d";
	private static final String DELIMITER = ";";
	private static final int RADIX = 36;

	private final int width;
	private final int depth;
	private String path;

	public FilePathGenerator(String paramsString) {
		this(paramsString.split(DELIMITER));
	}

	private FilePathGenerator(String ... params) {
		this(parseInt(params[0].replaceAll(" ", "")), parseInt(params[1].replaceAll(" ", "")));
	}

	public FilePathGenerator(int width, int depth) {
		this.width = width;
		this.depth = depth;
		if (width <= 0 || depth <= 0) {
			throw new IllegalArgumentException();
		}
	}

	private String dirName(StringBuilder dirBuilder, int width) {
		dirBuilder.setLength(0);
		dirBuilder.append(DIR_NAME_PREFIX);
		dirBuilder.append(Long.toString(nextLong(width), RADIX));
		return dirBuilder.toString();
	}

	private long nextLong(int range) {
		return abs(xorShift(System.nanoTime())) % range;
	}

	@Override
	public String get() {
		StringBuilder pathBuilder = new StringBuilder();
		StringBuilder dirBuilder = new StringBuilder();
		long newDepth = nextLong(depth) + 1;
		for (long i = 0; i < newDepth; i++) {
			pathBuilder.append(dirName(dirBuilder, width));
			pathBuilder.append('/');
		}
		return pathBuilder.toString();
	}
}
