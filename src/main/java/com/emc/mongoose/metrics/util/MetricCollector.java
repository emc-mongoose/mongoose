package com.emc.mongoose.metrics.util;

import io.prometheus.client.Collector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 @author veronika K. on 09.10.18 */
public class MetricCollector
	extends Collector {

	private double loQuantileValue = 0.25;
	private double hiQuantileValue = 0.75;
	private final Histogram histogram;
	private String metricName = String.valueOf(this.hashCode());
	private final List<String> labelValues = new ArrayList<>();
	private final List<String> labelNames = new ArrayList<>();
	private final LongAdder sum = new LongAdder();
	private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
	private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);
	//
	//TODO: public getters ?

	public MetricCollector(final int reservoirSize) {
		this.histogram = new Histogram(reservoirSize);
	}

	public MetricCollector() {
		this.histogram = new Histogram();
	}

	public void update(final int value) {
		update((long) value);
	}

	public void update(final long value) {
		histogram.update(value);
		sum.add(value);
		if(value < min.get()) {
			min.set(value);
		}
		if(value > max.get()) {
			max.set(value);
		}
	}

	public MetricCollector name(final String name) {
		metricName = name + "_" + hashCode();
		return this;
	}

	public MetricCollector loQ(final double q) {
		loQuantileValue = q;
		return this;
	}

	public MetricCollector hiQ(final double q) {
		hiQuantileValue = q;
		return this;
	}

	public MetricCollector label(final String name, final String value) {
		this.labelNames.add(name);
		this.labelValues.add(value);
		return this;
	}

	public MetricCollector labels(final String[] names, final String[] values) {
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
		final HistogramSnapshotImpl snapshot = histogram.snapshot(); //for quantieles
		samples.add(new MetricFamilySamples.Sample(metricName + "_count", labelNames, labelValues, count()));
		samples.add(new MetricFamilySamples.Sample(metricName + "_sum", labelNames, labelValues, sum()));
		samples.add(new MetricFamilySamples.Sample(metricName + "_max", labelNames, labelValues, max()));
		samples.add(new MetricFamilySamples.Sample(metricName + "_min", labelNames, labelValues, min()));
		samples.add(new MetricFamilySamples.Sample(metricName + "_mean", labelNames, labelValues, mean()));
		samples.add(new MetricFamilySamples.Sample(
			metricName + "_loQ", labelNames, labelValues, snapshot.quantile(loQuantileValue)));
		samples.add(new MetricFamilySamples.Sample(
			metricName + "_hiQ", labelNames, labelValues, snapshot.quantile(hiQuantileValue)));
		final MetricFamilySamples mfs = new MetricFamilySamples(metricName, Type.UNTYPED, "help", samples);
		final List<MetricFamilySamples> mfsList = new ArrayList<>(1);
		mfsList.add(mfs);
		return mfsList;
	}

	public HistogramSnapshotImpl snapshot(){
		return histogram.snapshot();
	}

	public List<String> labelNames() {
		return labelNames;
	}

	public List<String> labelValues() {
		return labelValues;
	}

	private long count() {
		return histogram.count();
	}

	private long sum() {
		return sum.sum();
	}

	private long max() {
		return max.get();
	}

	private long min() {
		return min.get();
	}

	private double mean() {
		return ((double) sum()) / count();
	}
}
