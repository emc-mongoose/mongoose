package com.emc.mongoose.metrics.util;

import java.io.Serializable;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.LongStream;

/**
 @author veronika K. on 03.10.18 */
public interface HistogramSnapshot
	extends Serializable {

	long quantile(final double quantile);

	int count();

	long[] values();

	long max();

	long min();

	long mean();

	long sum();

	long last();

	static HistogramSnapshot aggregate(final List<HistogramSnapshot> snapshots) {
		if(1 == snapshots.size()) {
			return snapshots.get(0);
		} else {
			final SortedSet<Long> sortedVals = new TreeSet<>();
			snapshots
				.stream()
				.map(HistogramSnapshot::values)
				.forEach(values -> LongStream.of(values).forEach(sortedVals::add));
			return new HistogramSnapshotImpl(sortedVals.stream().mapToLong(x -> x).toArray());
		}
	}
}
