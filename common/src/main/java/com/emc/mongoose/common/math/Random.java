package com.emc.mongoose.common.math;

import static com.emc.mongoose.common.math.MathUtil.xorShift;

public final class Random {
	
	private static final double DOUBLE_UNIT = 0x1.0p-53;

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
		return seed = xorShift(seed);
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
		seed = xorShift(seed);
		return (((seed >>> 22) << 27) + (seed >>> 21)) * DOUBLE_UNIT;
	}
}
