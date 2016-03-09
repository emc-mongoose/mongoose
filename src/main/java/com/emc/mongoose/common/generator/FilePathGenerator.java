package com.emc.mongoose.common.generator;

import com.emc.mongoose.common.math.MathUtil;

import java.util.Random;

import static com.emc.mongoose.common.math.MathUtil.xorShift;
import static java.lang.Integer.parseInt;
import static java.lang.Math.abs;

public class FilePathGenerator implements ValueGenerator<String> {

	public static final String DIR_NAME_PREFIX = "d";
	private static final int RADIX = 36;

	private String path;

	public FilePathGenerator(String paramsString) {
		this(paramsString.split(";"));

	}

	private FilePathGenerator(String ... params) {
		this(parseInt(params[0].replaceAll(" ", "")), parseInt(params[1].replaceAll(" ", "")));
	}

	public FilePathGenerator(int width, int depth) {
		if (width <= 0 || depth <= 0) {
			throw new IllegalArgumentException();
		}
		StringBuilder pathBuilder = new StringBuilder();
		StringBuilder dirBuilder = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			pathBuilder.append(dirName(dirBuilder, width));
			pathBuilder.append('/');
		}
		path = pathBuilder.toString();
	}

	private String dirName(StringBuilder dirBuilder, int width) {
		dirBuilder.setLength(0);
		dirBuilder.append(DIR_NAME_PREFIX);
		dirBuilder.append(Long.toString(abs(xorShift(System.nanoTime())) % RADIX, RADIX)); //todo in this way all go wrong
		return dirBuilder.toString();
	}

	@Override
	public String get() {
        return path;
	}
}
