package com.emc.mongoose.metrics.util;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;
import static java.lang.Math.min;

/**
 @author veronika K. on 28.09.18 */
public class ConcurrentSlidingWindowReservoir
implements Reservoir {

	private static final int DEFAULT_SIZE = 1028;
	private final long[] measurements;
	private final AtomicLong offset;

	public ConcurrentSlidingWindowReservoir(final int size) {
		this.measurements = new long[size];
		this.offset = new AtomicLong();
	}

	public ConcurrentSlidingWindowReservoir() {
		this(DEFAULT_SIZE);
	}

	@Override
	public int size() {
		return (int) min(offset.get(), measurements.length);
	}

	@Override
	public void update(final long value) {
		measurements[(int) (offset.incrementAndGet() % measurements.length)] = value;
	}

	@Override
	public HistogramSnapshotImpl snapshot() {
		final SortedSet<Long> sortedVals = new TreeSet<>();
		LongStream.of(measurements).forEach(sortedVals::add);
		return new HistogramSnapshotImpl(sortedVals.stream().mapToLong(x -> x).toArray());
	}
}
