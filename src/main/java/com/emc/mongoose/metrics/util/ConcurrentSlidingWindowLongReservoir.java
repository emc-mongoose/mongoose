package com.emc.mongoose.metrics.util;

import java.util.concurrent.atomic.AtomicLong;
import static java.lang.Math.min;

/**
 @author veronika K. on 28.09.18 */
public class ConcurrentSlidingWindowLongReservoir
implements LongReservoir {

	private static final int DEFAULT_SIZE = 1028;
	private final long[] measurements;
	private final AtomicLong offset;

	public ConcurrentSlidingWindowLongReservoir(final int size) {
		this.measurements = new long[size];
		this.offset = new AtomicLong();
	}

	public ConcurrentSlidingWindowLongReservoir() {
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
	public long[] snapshot() {
		return measurements.clone();
	}
}
