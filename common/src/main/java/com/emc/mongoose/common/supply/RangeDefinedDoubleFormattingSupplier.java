package com.emc.mongoose.common.supply;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

/**
 Created by kurila on 07.03.17.
 */
public final class RangeDefinedDoubleFormattingSupplier
extends RangeDefinedDoubleSupplier
implements RangeDefinedSupplier<Double> {
	
	private final NumberFormat format;
	
	public RangeDefinedDoubleFormattingSupplier(
		final long seed, final double min, final double max, final String formatStr
	) {
		super(seed, min, max);
		this.format = formatStr == null || formatStr.isEmpty() ?
			null : new DecimalFormat(formatStr);
	}
	
	@Override
	public final String get() {
		return format == null ? Double.toString(getAsDouble()) : format.format(getAsDouble());
	}
	
	@Override
	public final int get(final List<String> buffer, final int limit) {
		final double numbers[] = new double[limit];
		final int n = super.get(numbers, limit);
		if(format == null) {
			for(int i = 0; i < n; i ++) {
				buffer.add(Double.toString(numbers[i]));
			}
		} else {
			for(int i = 0; i < n; i ++) {
				buffer.add(format.format(numbers[i]));
			}
		}
		return n;
	}
	
	@Override
	public final Double value() {
		return getAsDouble();
	}
	
	@Override
	public final void close()
	throws IOException {
		super.close();
	}
}
