package com.emc.mongoose.metrics.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

/**
 @author veronika K. on 01.10.18 */
public class Histogram {

	private final ConcurrentSlidingWindowReservoir reservoir;
	private final LongAdder count;

	public Histogram(final ConcurrentSlidingWindowReservoir reservoir) {
		this.reservoir = reservoir;
		this.count = new LongAdder();
	}

	public Histogram(final int reservoirSize) {
		this(new ConcurrentSlidingWindowReservoir(reservoirSize));
	}

	public Histogram() {
		this(new ConcurrentSlidingWindowReservoir());
	}

	public void update(final int value) {
		update((long) value);
	}

	public void update(final long value) {
		count.increment();
		reservoir.update(value);
	}

	public long count() {
		return count.longValue();
	}

	public HistogramSnapshotImpl snapshot() {
		return reservoir.snapshot();
	}
}
