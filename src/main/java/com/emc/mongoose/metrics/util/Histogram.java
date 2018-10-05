package com.emc.mongoose.metrics.util;

import io.prometheus.client.Collector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

/**
 @author veronika K. on 01.10.18 */
public class Histogram
	extends Collector {

	private final ConcurrentSlidingWindowReservoir reservoir;
	private final LongAdder count;
	private String metricName = String.valueOf(this.hashCode());
	private final String labelName = "STEP_ID";
	private String labelValue = String.valueOf(this.hashCode());

	public Histogram(final ConcurrentSlidingWindowReservoir reservoir) {
		this.reservoir = reservoir;
		this.count = new LongAdder();
	}

	public Histogram(final int reservoirSize) {
		this.reservoir = new ConcurrentSlidingWindowReservoir(reservoirSize);
		this.count = new LongAdder();
	}

	public void update(final int value) {
		update((long) value);
	}

	public void update(final long value) {
		count.increment();
		reservoir.update(value);
	}

	public long count() {
		return count.sum();
	}

	public HistogramSnapshotImpl snapshot() {
		return reservoir.snapshot();
	}

	public Histogram name(final String name) {
		metricName = name + "_" + hashCode();
		return this;
	}

	public Histogram labelValue(final String value) {
		labelValue = value;
		return this;
	}

	@Override
	public List<MetricFamilySamples> collect() {
		List<MetricFamilySamples.Sample> samples = new ArrayList<MetricFamilySamples.Sample>();
		final List<String> labelNames = Arrays.asList(labelName);
		final List<String> labelValues = Arrays.asList(labelValue);
		final HistogramSnapshotImpl snapshot = snapshot();
		samples.add(new MetricFamilySamples.Sample(metricName + "_count", labelNames, labelValues, snapshot.count()));
		samples.add(new MetricFamilySamples.Sample(metricName + "_sum", labelNames, labelValues, snapshot.sum()));
		samples.add(new MetricFamilySamples.Sample(metricName + "_max", labelNames, labelValues, snapshot.max()));
		samples.add(new MetricFamilySamples.Sample(metricName + "_min", labelNames, labelValues, snapshot.min()));
		samples.add(new MetricFamilySamples.Sample(metricName + "_mean", labelNames, labelValues, snapshot.mean()));
		MetricFamilySamples mfs = new MetricFamilySamples(metricName, Type.UNTYPED, "help", samples);
		final List<MetricFamilySamples> mfsList = new ArrayList<>(1);
		mfsList.add(mfs);
		return mfsList;
	}

	public String labelName() {
		return labelName;
	}

	public String labelValue() {
		return labelValue;
	}
}
