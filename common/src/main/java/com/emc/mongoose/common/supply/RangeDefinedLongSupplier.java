package com.emc.mongoose.common.supply;

import com.emc.mongoose.common.math.Random;
import com.emc.mongoose.common.supply.BatchLongSupplier;

import java.io.IOException;

/**
 Created by kurila on 07.03.17.
 */
public class RangeDefinedLongSupplier
implements BatchLongSupplier {
	
	private final long min;
	private final long range;
	
	/**
	 @param min "unbounded" range will be used if Long.MAX_VALUE > max - min
	 @param max "unbounded" range will be if less than min
	 */
	public RangeDefinedLongSupplier(final long min, final long max) {
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
	public final long getAsLong() {
		if(range < 0) {
			return RND.get().nextLong();
		} else {
			return min + RND.get().nextLong(range);
		}
	}
	
	@Override
	public final int get(final long[] buffer, final int limit) {
		final Random rnd = RND.get();
		final int _limit = Math.min(buffer.length, limit);
		if(range < 0) {
			for(int i = 0; i < _limit; i ++) {
				buffer[i] = rnd.nextLong();
			}
		} else {
			for(int i = 0; i < _limit; i ++) {
				buffer[i] = min + rnd.nextLong(range);
			}
		}
		return _limit;
	}
	
	@Override
	public final long skip(final long count) {
		final Random rnd = RND.get();
		for(long i = 0; i < count; i ++) {
			rnd.nextLong();
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
