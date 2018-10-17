package com.emc.mongoose.metrics.util;

import java.util.Arrays;

import static java.lang.Math.floor;

/**
 @author veronika K. on 25.09.18 */
public class HistogramSnapshotImpl
implements HistogramSnapshot {

	private final long[] sortedVals;

	public HistogramSnapshotImpl(final long[] sortedVals) {
		this.sortedVals = sortedVals;
		Arrays.sort(this.sortedVals);
	}

	@Override
	public long quantile(final double quantile) {
		if(quantile < 0.0 || quantile > 1.0 || Double.isNaN(quantile)) {
			throw new IllegalArgumentException(quantile + " is not in [0..1]");
		}
		if(sortedVals.length == 0) {
			return 0;
		}
		final long pos = new Double(quantile * (sortedVals.length + 1)).longValue();
		final int index = (int) pos;
		if(index < 1) {
			return sortedVals[0];
		}
		if(index >= sortedVals.length) {
			return sortedVals[sortedVals.length - 1];
		}
		final long lower = sortedVals[index - 1];
		final long upper = sortedVals[index];
		return lower + (pos - new Double(floor(pos)).longValue()) * (upper - lower);
	}

	@Override
	public int count() {
		return sortedVals.length;
	}

	@Override
	public long[] values() {
		return sortedVals;
	}

	@Override
	public long max() {
		if(sortedVals.length == 0) {
			return 0;
		}
		return sortedVals[sortedVals.length - 1];
	}

	@Override
	public long min() {
		if(sortedVals.length == 0) {
			return 0;
		}
		return sortedVals[0];
	}

	@Override
	public long mean() {
		if(sortedVals.length == 0) {
			return 0;
		}
		return sum() / sortedVals.length;
	}

	public long median() {
		return quantile(0.5);
	}

	@Override
	public long sum() {
		long sum = 0;
		for(int i = 0; i < sortedVals.length; ++ i) {
			sum += sortedVals[i];
		}
		return sum;
	}

	@Override
	public long last() {
		return sortedVals[sortedVals.length - 1];
	}
}
