package com.emc.mongoose.metrics.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 @author veronika K. on 10.10.18 */
public class TimingMeterImpl
	implements TimingMeter {

	private final HistogramImpl histogramImpl;
	private final LongAdder count = new LongAdder();
	private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
	private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);
	//
	private String metricName;
	//private final String metricName;
	//count,max,min,sum,mean,histogram

	public TimingMeterImpl(final int reservoirSize, final String metricName) {
		this(new HistogramImpl(reservoirSize), metricName);
	}

	public TimingMeterImpl(final HistogramImpl histogramImpl, final String metricName) {
		this.histogramImpl = histogramImpl;
		this.metricName = metricName;
	}

	public void update(final int value) {
		update((long) value);
	}

	public void update(final long value) {
		histogramImpl.update(value);
		count.add(value);
		if(value < min.get()) {
			min.set(value);
		}
		if(value > max.get()) {
			max.set(value);
		}
	}

	public String name() {
		return metricName;
	}

	public SingleMetricSnapshot snapshot() {
		return new TimingMetricSnapshotImpl(sum(), count(), min(), max(), mean(), histogramImpl.snapshot(), metricName);
	}

	public long sum() {
		return count.sum();
	}

	public long min() {
		return min.longValue();
	}

	public long max() {
		return max.longValue();
	}

	public long count() {
		return count.longValue();
	}

	public double mean() {
		return ((double) sum()) / count();
	}
}
