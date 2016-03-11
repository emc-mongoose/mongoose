package com.emc.mongoose.common.generator;

import java.io.File;

import static com.emc.mongoose.common.math.MathUtil.xorShift;
import static java.lang.Integer.parseInt;
import static java.lang.Math.abs;

public class FilePathGenerator
implements ValueGenerator<String> {

	private static final String DELIMITER = ";";
	private static final int RADIX = Character.MAX_RADIX;

	private final int width;
	private final int depth;

	public long seed = System.nanoTime() ^ System.currentTimeMillis();

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

	private int nextNumber(final int range) {
		seed = xorShift(seed) ^ System.nanoTime();
		return (int) abs(seed % range);
	}

	private String nextDirName(final int width) {
		return Long.toString(nextNumber(width), RADIX);
	}

	private final static ThreadLocal<StringBuilder>
		THREAD_LOCAL_PATH_PUILDER = new ThreadLocal<StringBuilder>() {
			@Override
			protected StringBuilder initialValue() {
				return new StringBuilder();
			}
		};

	@Override
	public String get() {
		final StringBuilder pathBuilder = THREAD_LOCAL_PATH_PUILDER.get();
		pathBuilder.setLength(0);
		final long newDepth = nextNumber(depth) + 1;
		for(long i = 0; i < newDepth; i++) {
			pathBuilder.append(nextDirName(width));
			pathBuilder.append(File.separatorChar);
		}
		return pathBuilder.toString();
	}
}
