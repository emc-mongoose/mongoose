package com.emc.mongoose.common.supply;

import com.emc.mongoose.common.math.Random;
import static com.emc.mongoose.common.io.Input.DELIMITER;

import java.io.File;
import java.util.List;
import static java.lang.Integer.parseInt;

public class FilePathSupplier
implements BatchSupplier<String> {

	private static final int RADIX = Character.MAX_RADIX;

	private final Random rnd;
	private final int width;
	private final int depth;

	public FilePathSupplier(final long seed, final String paramsString) {
		this(seed, areParamsValid(paramsString) ? paramsString.split(DELIMITER) : new String[]{});
	}

	private FilePathSupplier(final long seed, final String[] params) {
		this(
			seed,
			(params.length > 0 ? parseInt(params[0].replaceAll(" ", "")) : 0),
			(params.length > 1 ? parseInt(params[1].replaceAll(" ", "")) : 0)
		);
	}
	
	public FilePathSupplier(final long seed, int width, int depth) {
		this.rnd = new Random(seed);
		this.width = width;
		this.depth = depth;
		if(width <= 0 || depth <= 0) {
			throw new IllegalArgumentException();
		}
	}
	
	private static final ThreadLocal<StringBuilder>
		THREAD_LOCAL_PATH_BUILDER = new ThreadLocal<StringBuilder>() {
			@Override
			protected StringBuilder initialValue() {
				return new StringBuilder();
			}
		};

	@Override
	public final String get() {
		final StringBuilder pathBuilder = THREAD_LOCAL_PATH_BUILDER.get();
		pathBuilder.setLength(0);
		final int newDepth = rnd.nextInt(depth) + 1;
		for(int i = 0; i < newDepth; i++) {
			pathBuilder.append(nextDirName(width));
			pathBuilder.append(File.separatorChar);
		}
		return pathBuilder.toString();
	}
	
	@Override
	public final int get(final List<String> buffer, final int limit) {
		int count = 0, newDepth;
		final StringBuilder pathBuilder = THREAD_LOCAL_PATH_BUILDER.get();
		for(; count < limit; count ++) {
			pathBuilder.setLength(0);
			newDepth = rnd.nextInt(depth) + 1;
			for(int i = 0; i < newDepth; i++) {
				pathBuilder.append(nextDirName(width));
				pathBuilder.append(File.separatorChar);
			}
			buffer.add(pathBuilder.toString());
		}
		return count;
	}
	
	@Override
	public final long skip(final long count) {
		for(long i = 0; i < count; i ++) {
			rnd.nextInt(depth);
		}
		return count;
	}
	
	@Override
	public final void reset() {
		rnd.reset();
	}
	
	@Override
	public final void close() {
	}
	
	private static boolean areParamsValid(final String paramsString) {
		final int delPos;
		if(paramsString == null) {
			delPos = 0;
		} else {
			delPos = paramsString.indexOf(DELIMITER);
		}
		return delPos > 0 && delPos < paramsString.length() - 1;
	}
	
	private String nextDirName(final int width) {
		return Integer.toString(Math.abs(rnd.nextInt(width)), RADIX);
	}
}
