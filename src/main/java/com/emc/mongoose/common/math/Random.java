package com.emc.mongoose.common.math;

import static com.emc.mongoose.common.math.MathUtil.xorShift;

public final class Random {

	private static final java.util.Random JAVA_RANDOM = new java.util.Random();

	private long seed;

	public Random() {
		reset();
	}

	public void reset() {
		seed = System.nanoTime() ^ System.currentTimeMillis();
	}

	public Random(final long seed) {
		this.seed = seed;
	}

	public final long nextLong() {
		return seed = xorShift(seed) ^ System.nanoTime();
	}

	public final long nextLong(final long range) {
		return Math.abs(nextLong() % range);
	}

	public final int nextInt() {
		return (int) nextLong();
	}

	public final int nextInt(final int range) {
		return (int) nextLong(range);
	}

	public final double nextDouble() {
		return JAVA_RANDOM.nextDouble();
	}
}
