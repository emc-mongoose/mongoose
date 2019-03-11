package com.emc.mongoose.base.metrics.type;

import com.emc.mongoose.base.metrics.snapshot.ConcurrencyMetricSnapshot;
import com.emc.mongoose.base.metrics.snapshot.ConcurrencyMetricSnapshotImpl;

import java.util.concurrent.atomic.LongAdder;

public class ConcurrencyMeterImpl implements LongMeter<ConcurrencyMetricSnapshot> {

	private final String name;
	private final LongAdder counter = new LongAdder();
	private final LongAdder sumAdder = new LongAdder();
	private volatile long last = 0;

	public ConcurrencyMeterImpl(final String name) {
		this.name = name;
	}

	@Override
	public final void update(final long v) {
		counter.increment();
		sumAdder.add(v);
		last = v;
	}

	@Override
	public final ConcurrencyMetricSnapshotImpl snapshot() {
		final long count = counter.sum();
		final double mean = count > 0 ? ((double) sumAdder.sum()) / count : 0;
		return new ConcurrencyMetricSnapshotImpl(name, last, mean);
	}
}
