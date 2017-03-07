package com.emc.mongoose.common.supply;

import java.io.IOException;
import java.util.List;

/**
 Created by kurila on 07.03.17.
 */
public final class RangeDefinedLongFormattingSupplier
extends RangeDefinedLongSupplier
implements RangeDefinedSupplier<Long> {
	
	public RangeDefinedLongFormattingSupplier() {
		super(Long.MIN_VALUE, Long.MAX_VALUE);
	}
	
	public RangeDefinedLongFormattingSupplier(final long min, final long max) {
		super(min, max);
	}
	
	@Override
	public String get() {
		return Long.toString(getAsLong());
	}
	
	@Override
	public int get(final List<String> buffer, final int limit) {
		final long numbers[] = new long[limit];
		final int n = super.get(numbers, limit);
		for(int i = 0; i < n; i ++) {
			buffer.add(Long.toString(numbers[i]));
		}
		return n;
	}
	
	@Override
	public Long value() {
		return getAsLong();
	}
	
	@Override
	public final void close()
	throws IOException {
		super.close();
	}
}
