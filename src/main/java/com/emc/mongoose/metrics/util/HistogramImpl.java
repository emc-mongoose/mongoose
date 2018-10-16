package com.emc.mongoose.metrics.util;

import java.util.concurrent.atomic.LongAdder;

/**
 @author veronika K. on 01.10.18 */
public class HistogramImpl
	implements Histogram {

	private final ConcurrentSlidingWindowReservoir reservoir;
	private final LongAdder count;

	public HistogramImpl(final ConcurrentSlidingWindowReservoir reservoir) {
		this.reservoir = reservoir;
		this.count = new LongAdder();
	}

	public HistogramImpl(final int reservoirSize) {
		this(new ConcurrentSlidingWindowReservoir(reservoirSize));
	}

	public HistogramImpl() {
		this(new ConcurrentSlidingWindowReservoir());
	}

	public HistogramImpl(final long[] values) {
		this();
		for(int i = 0; i < values.length; ++ i) {
			update(values[i]);
		}
	}

	@Override
	public void update(final int value) {
		update((long) value);
	}

	@Override
	public void update(final long value) {
		count.increment();
		reservoir.update(value);
	}

	@Override
	public long count() {
		return count.longValue();
	}

	@Override
	public HistogramSnapshotImpl snapshot() {
		return reservoir.snapshot();
	}
}
