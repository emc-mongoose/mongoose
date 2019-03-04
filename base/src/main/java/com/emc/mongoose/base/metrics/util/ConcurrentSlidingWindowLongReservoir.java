package com.emc.mongoose.base.metrics.util;

import static java.lang.Math.min;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/** @author veronika K. on 28.09.18 */
public class ConcurrentSlidingWindowLongReservoir implements LongReservoir {

	private static final int DEFAULT_SIZE = 1028;
	private final long[] measurements;
	private final AtomicLong count;

	public ConcurrentSlidingWindowLongReservoir(final int size) {
		this.measurements = new long[size];
		this.count = new AtomicLong();
	}

	public ConcurrentSlidingWindowLongReservoir() {
		this(DEFAULT_SIZE);
	}

	@Override
	public int size() {
		return (int) min(count.get(), measurements.length);
	}

	@Override
	public void update(final long value) {
		measurements[(int) (count.getAndIncrement() % measurements.length)] = value;
	}

	@Override
	public long[] snapshot() {
		final long countSnapshot = count.get();
		if (countSnapshot < measurements.length) {
			return Arrays.copyOf(measurements, (int) countSnapshot);
		} else {
			return measurements.clone();
		}
	}
}
