package com.emc.mongoose.metrics;

import java.util.concurrent.atomic.AtomicLong;

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
	public Snapshot snapshot() {
		final long[] values = new long[size()];
		for(int i = 0; i < values.length; i++) {
			values[i] = measurements[i];
		}
		return new Snapshot(values);
	}
}
