package com.emc.mongoose.common.supply;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

/**
 Created by kurila on 07.03.17.
 */
public final class RangeDefinedLongFormattingSupplier
extends RangeDefinedLongSupplier
implements RangeDefinedSupplier<Long> {
	
	private final NumberFormat format;
	
	public RangeDefinedLongFormattingSupplier(
		final long seed, final long min, final long max, final String formatStr
	) {
		super(seed, min, max);
		this.format = formatStr == null || formatStr.isEmpty() ?
			null : new DecimalFormat(formatStr);
	}
	
	@Override
	public String get() {
		return format == null ? Long.toString(getAsLong()) : format.format(getAsLong());
	}
	
	@Override
	public int get(final List<String> buffer, final int limit) {
		final long numbers[] = new long[limit];
		final int n = super.get(numbers, limit);
		if(format == null) {
			for(int i = 0; i < n; i ++) {
				buffer.add(Long.toString(numbers[i]));
			}
		} else {
			for(int i = 0; i < n; i ++) {
				buffer.add(format.format(numbers[i]));
			}
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
