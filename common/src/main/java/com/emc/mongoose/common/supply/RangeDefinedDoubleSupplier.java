package com.emc.mongoose.common.supply;

import com.emc.mongoose.common.math.Random;

import java.io.IOException;

/**
 Created by kurila on 07.03.17.
 */
public class RangeDefinedDoubleSupplier
implements BatchDoubleSupplier {
	
	private final double min;
	private final double range;
	private final Random rnd;
	
	public RangeDefinedDoubleSupplier(final long seed, final double min, final double max) {
		rnd = new Random(seed);
		this.min = min;
		this.range = max - min;
	}

	@Override
	public final double getAsDouble() {
		if(range < 0) {
			return rnd.nextDouble();
		} else {
			return min + range * rnd.nextDouble();
		}
	}
	
	@Override
	public final int get(final double[] buffer, final int limit) {
		final int _limit = Math.min(buffer.length, limit);
		if(range < 0) {
			for(int i = 0; i < _limit; i ++) {
				buffer[i] = rnd.nextDouble();
			}
		} else {
			for(int i = 0; i < _limit; i ++) {
				buffer[i] = min + range * rnd.nextDouble();
			}
		}
		return _limit;
	}
	
	@Override
	public final long skip(final long count) {
		for(long i = 0; i < count; i ++) {
			rnd.nextDouble();
		}
		return count;
	}
	
	@Override
	public final void reset() {
		rnd.reset();
	}
	
	@Override
	public void close()
	throws IOException {
	}
}
