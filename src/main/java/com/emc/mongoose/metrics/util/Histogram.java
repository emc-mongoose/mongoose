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
	private final List<String> labelValues = new ArrayList<>();
	private final List<String> labelNames = new ArrayList<>();
//	private final Map<String, String> labels = new HashMap<>();

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

	public Histogram label(final String name, final String value) {
		this.labelNames.add(name);
		this.labelValues.add(value);
		return this;
	}

	public Histogram labels(final String[] names, final String[] values) {
		if(names.length != values.length) {
			throw new IllegalArgumentException(
				"The number of label names(" + names.length +
					") does not match the number of values(" + values.length + ")");
		}
		this.labelNames.addAll(Arrays.asList(names));
		this.labelValues.addAll(Arrays.asList(values));
		return this;
	}

	@Override
	public List<MetricFamilySamples> collect() {
		final List<MetricFamilySamples.Sample> samples = new ArrayList<MetricFamilySamples.Sample>();
		final HistogramSnapshotImpl snapshot = snapshot();
		samples.add(new MetricFamilySamples.Sample(metricName + "_count", labelNames, labelValues, snapshot.count()));
		samples.add(new MetricFamilySamples.Sample(metricName + "_sum", labelNames, labelValues, snapshot.sum()));
		samples.add(new MetricFamilySamples.Sample(metricName + "_max", labelNames, labelValues, snapshot.max()));
		samples.add(new MetricFamilySamples.Sample(metricName + "_min", labelNames, labelValues, snapshot.min()));
		samples.add(new MetricFamilySamples.Sample(metricName + "_mean", labelNames, labelValues, snapshot.mean()));
		final MetricFamilySamples mfs = new MetricFamilySamples(metricName, Type.UNTYPED, "help", samples);
		final List<MetricFamilySamples> mfsList = new ArrayList<>(1);
		mfsList.add(mfs);
		return mfsList;
	}

	public List<String> labelNames() {
		return labelNames;
	}

	public List<String> labelValues() {
		return labelValues;
	}
}
