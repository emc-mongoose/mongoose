package com.emc.mongoose.metrics;

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
	public void update(int value) {
		update((long) value);
	}

	public void update(long value) {
		count.increment();
		reservoir.update(value);
	}

	public long count() {
		return count.sum();
	}

	public Snapshot snapshot() {
		return reservoir.snapshot();
	}
}
