package com.emc.mongoose.metrics.util;

import java.util.Arrays;
import java.util.List;

import static java.lang.Math.floor;

/**
 @author veronika K. on 25.09.18 */
public class HistogramSnapshotImpl
	implements HistogramSnapshot {

	private final long[] values;

	public HistogramSnapshotImpl(final long[] values) {
		this.values = Arrays.copyOf(values, values.length);
		Arrays.sort(this.values);
	}

	public HistogramSnapshotImpl(final List<HistogramSnapshotImpl> snapshots) {
		int size = 0;
		for(final HistogramSnapshotImpl s : snapshots) {
			size += s.count();
		}
		this.values = new long[size];
		int index_s = 0;
		for(final HistogramSnapshotImpl s : snapshots) {
			final long[] copy = Arrays.copyOf(s.values(), s.count());
			for(int i = 0; i < copy.length; i++) {
				this.values[index_s * i + i] = copy[i];
			}
			++ index_s;
		}
		Arrays.sort(this.values);
	}

	@Override
	public long quantile(final double quantile) {
		if(quantile < 0.0 || quantile > 1.0 || Double.isNaN(quantile)) {
			throw new IllegalArgumentException(quantile + " is not in [0..1]");
		}
		if(values.length == 0) {
			return 0;
		}
		final long pos = new Double(quantile * (values.length + 1)).longValue();
		final int index = (int) pos;
		if(index < 1) {
			return values[0];
		}
		if(index >= values.length) {
			return values[values.length - 1];
		}
		final long lower = values[index - 1];
		final long upper = values[index];
		return lower + (pos - new Double(floor(pos)).longValue()) * (upper - lower);
	}

	@Override
	public int count() {
		return values.length;
	}

	@Override
	public long[] values() {
		return Arrays.copyOf(values, values.length);
	}

	@Override
	public long max() {
		if(values.length == 0) {
			return 0;
		}
		return values[values.length - 1];
	}

	@Override
	public long min() {
		if(values.length == 0) {
			return 0;
		}
		return values[0];
	}

	@Override
	public long mean() {
		if(values.length == 0) {
			return 0;
		}
		return sum() / values.length;
	}

	public long median() {
		return quantile(0.5);
	}

	@Override
	public long sum() {
		long sum = 0;
		for(int i = 0; i < values.length; ++ i) {
			sum += values[i];
		}
		return sum;
	}
}
