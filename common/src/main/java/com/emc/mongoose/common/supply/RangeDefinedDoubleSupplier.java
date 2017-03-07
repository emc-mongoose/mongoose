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
	
	public RangeDefinedDoubleSupplier(final double min, final double max) {
		this.min = min;
		this.range = max - min;
	}
	
	private static ThreadLocal<Random> RND = new ThreadLocal<Random>() {
		@Override
		protected final Random initialValue() {
			return new Random();
		}
	};
	
	@Override
	public final double getAsDouble() {
		if(range < 0) {
			return RND.get().nextDouble();
		} else {
			return min + range * RND.get().nextDouble();
		}
	}
	
	@Override
	public final int get(final double[] buffer, final int limit) {
		final Random rnd = RND.get();
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
		final Random rnd = RND.get();
		for(long i = 0; i < count; i ++) {
			rnd.nextDouble();
		}
		return count;
	}
	
	@Override
	public final void reset() {
		RND.get().reset();
	}
	
	@Override
	public void close()
	throws IOException {
	}
}
