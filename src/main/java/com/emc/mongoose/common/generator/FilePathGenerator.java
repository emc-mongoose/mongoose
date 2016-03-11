package com.emc.mongoose.common.generator;

import com.emc.mongoose.common.math.Random;

import java.io.File;

import static java.lang.Integer.parseInt;

public class FilePathGenerator
implements ValueGenerator<String> {

	public static final String DELIMITER = ";";
	private static final int RADIX = Character.MAX_RADIX;
	private static final Random RANDOM = new Random();

	private final int width;
	private final int depth;

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

	private String nextDirName(final int width) {
		return Long.toString(RANDOM.nextNumber(width), RADIX);
	}

	private final static ThreadLocal<StringBuilder>
		THREAD_LOCAL_PATH_BUILDER = new ThreadLocal<StringBuilder>() {
			@Override
			protected StringBuilder initialValue() {
				return new StringBuilder();
			}
		};

	@Override
	public String get() {
		final StringBuilder pathBuilder = THREAD_LOCAL_PATH_BUILDER.get();
		pathBuilder.setLength(0);
		final long newDepth = RANDOM.nextNumber(depth) + 1;
		for(long i = 0; i < newDepth; i++) {
			pathBuilder.append(nextDirName(width));
			pathBuilder.append(File.separatorChar);
		}
		return pathBuilder.toString();
	}
}
