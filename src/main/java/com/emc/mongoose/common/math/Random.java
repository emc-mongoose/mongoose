package com.emc.mongoose.common.math;

import static java.lang.Math.abs;
import static com.emc.mongoose.common.math.MathUtil.xorShift;

public class Random {

	private long seed;

	public Random() {
		seed = System.nanoTime() ^ System.currentTimeMillis();
	}

	public Random (final long seed) {
		this.seed = seed;
	}

	public int nextNumber(final int range) {
		seed = xorShift(seed) ^ System.nanoTime();
		return (int) abs(seed % range);
	}
}
