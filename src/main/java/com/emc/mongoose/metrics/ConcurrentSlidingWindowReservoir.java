package com.emc.mongoose.metrics;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.UniformSnapshot;

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Math.min;

public class ConcurrentSlidingWindowReservoir
	implements Reservoir {

	private static final int DEFAULT_SIZE = 1028;
	private final long[] measurements;
	private final AtomicLong offset;

	public ConcurrentSlidingWindowReservoir(int size) {
		this.measurements = new long[size];
		this.offset = new AtomicLong();
	}

	public ConcurrentSlidingWindowReservoir() {
		this.measurements = new long[DEFAULT_SIZE];
		this.offset = new AtomicLong();
	}
	
	public int offset(){
		return offset.intValue();
	}

	@Override
	public int size() {
		return (int) min(offset.get(), measurements.length);
	}

	@Override
	public void update(long value) {
		measurements[(int) (offset.incrementAndGet() % measurements.length)] = value;
	}

	@Override
	public Snapshot getSnapshot() {
		final long[] values = new long[size()];
		for(int i = 0; i < values.length; i++) {
			synchronized(this) {
				values[i] = measurements[i];
			}
		}
		return new UniformSnapshot(values);
	}
}
