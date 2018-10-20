package com.emc.mongoose.metrics.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 @author veronika K. on 10.10.18 */
public class TimingMeterImpl<S extends TimingMetricSnapshot>
implements TimingMeter<S> {

	private final Histogram histogram;
	private final LongAdder count = new LongAdder();
	private final LongAdder sum = new LongAdder();
	private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
	private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);
	private String metricName;

	public TimingMeterImpl(final Histogram histogram, final String metricName) {
		this.histogram = histogram;
		this.metricName = metricName;
	}

	@Override
	public void update(final long value) {
		histogram.update(value);
		count.increment();
		sum.add(value);
		if(value < min.get()) {
			min.set(value);
		}
		if(value > max.get()) {
			max.set(value);
		}
	}

	@Override
	public String name() {
		return metricName;
	}

	@Override
	public S snapshot() {
		if(sum() == 0) {
			return (S) new TimingMetricSnapshotImpl(0, 0, 0, 0, 0, histogram.snapshot(), "");
		}
		return (S) new TimingMetricSnapshotImpl(
			sum(), count(), min(), max(), mean(), histogram.snapshot(), metricName
		);
	}

	@Override
	public long sum() {
		return sum.sum();
	}

	@Override
	public long min() {
		return min.longValue();
	}

	@Override
	public long max() {
		return max.longValue();
	}

	@Override
	public long count() {
		return count.sum();
	}

	@Override
	public double mean() {
		return (sum() == 0) ? 0 : ((double) sum()) / count();
	}
}
