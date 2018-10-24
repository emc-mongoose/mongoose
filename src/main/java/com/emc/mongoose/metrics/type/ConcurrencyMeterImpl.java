package com.emc.mongoose.metrics.type;

import com.emc.mongoose.metrics.snapshot.ConcurrencyMetricSnapshot;
import com.emc.mongoose.metrics.snapshot.ConcurrencyMetricSnapshotImpl;

import java.util.concurrent.atomic.LongAdder;

public class ActualConcurrencyMeterImpl
implements LongMeter<ConcurrencyMetricSnapshot> {

	private final String name;
	private final LongAdder counter = new LongAdder();
	private final LongAdder sumAdder = new LongAdder();
	private volatile long last = 0;

	public ActualConcurrencyMeterImpl(final String name) {
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
		final long mean = count > 0 ? sumAdder.sum() / count : 0;
		return new ConcurrencyMetricSnapshotImpl(name, last, mean);
	}
}
