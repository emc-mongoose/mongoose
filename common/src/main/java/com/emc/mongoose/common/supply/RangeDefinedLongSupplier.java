package com.emc.mongoose.common.supply;

import com.emc.mongoose.common.math.Random;
import static com.emc.mongoose.common.supply.RangeDefinedSupplier.SHARED_SEED;

import java.io.IOException;

/**
 Created by kurila on 07.03.17.
 */
public class RangeDefinedLongSupplier
implements BatchLongSupplier {
	
	private final long min;
	private final long range;
	private final Random rnd = new Random(SHARED_SEED);
	
	/**
	 @param min "unbounded" range will be used if Long.MAX_VALUE &gt; max - min
	 @param max "unbounded" range will be if less than min
	 */
	public RangeDefinedLongSupplier(final long min, final long max) {
		this.min = min;
		this.range = max - min + 1;
	}

	@Override
	public final long getAsLong() {
		if(range < 1) {
			return rnd.nextLong();
		} else {
			return min + rnd.nextLong(range);
		}
	}
	
	@Override
	public final int get(final long[] buffer, final int limit) {
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
		for(long i = 0; i < count; i ++) {
			rnd.nextLong();
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
