package com.emc.mongoose.base.config.el;

import com.github.akurilov.commons.math.Random;

public final class RandomPath {

	public static final char PATH_SEP = '/';
	public static final int RADIX = Character.MAX_RADIX;
	private static final ThreadLocal<StringBuilder> THREAD_LOCAL_PATH_BUILDER = ThreadLocal.withInitial(StringBuilder::new);
	private static final ThreadLocal<Random> THREAD_LOCAL_RND = ThreadLocal.withInitial(Random::new);

	private RandomPath() {}

	public static String get(final int width, final int depth) {
		final var pathBuilder = THREAD_LOCAL_PATH_BUILDER.get();
		pathBuilder.setLength(0);
		final var rnd = THREAD_LOCAL_RND.get();
		final var newDepth = rnd.nextInt(depth) + 1;
		for (var i = 0; i < newDepth; i++) {
			final var dirName = Integer.toUnsignedString(rnd.nextInt(width), RADIX);
			pathBuilder.append(dirName);
			pathBuilder.append(PATH_SEP);
		}
		return pathBuilder.toString();
	}
}
