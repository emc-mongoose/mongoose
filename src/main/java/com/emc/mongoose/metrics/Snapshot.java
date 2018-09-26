package com.emc.mongoose.metrics;

import io.prometheus.client.Collector;
import io.prometheus.client.Summary;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;

/**
 @author veronika K. on 25.09.18 */
public class Snapshot {

	final private double mean;
	final private double count;
	final private double sum;
	final private double med;
	final private double hiQ;
	final private double loQ;
	final private double min;
	final private double max;

	public Snapshot(
		final double mean, final double sum, final double med, final double hiQ, final double loQ,
		final double min, final double max
	) {
		this.mean = mean;
		this.count = (double) new Double(sum / mean).longValue();
		this.sum = sum;
		this.med = med;
		this.hiQ = hiQ;
		this.loQ = loQ;
		this.min = min;
		this.max = max;
	}

	public Snapshot(final Summary summary) {
		final String metricName = summary.collect().get(0).name;
		final List<Collector.MetricFamilySamples.Sample> samples = summary.collect().get(0).samples;
		this.sum = samples.stream().filter(metric -> metric.name.equals(metricName + "_sum")).findFirst().get().value;
		this.count = samples.stream().filter(
			metric -> metric.name.equals(metricName + "_count")).findFirst().get().value;
		this.mean = this.sum / this.count;
		final BiFunction<Double, Collector.MetricFamilySamples.Sample, Boolean> isQuantile = (q, metric)
			-> metric.name.equals(metricName)
			&& metric.labelNames.contains("quantile")
			&& metric.labelValues.contains(q.toString());
		this.med = samples.stream().filter(m -> isQuantile.apply(0.5, m)).findFirst().get().value;
		this.hiQ = samples.stream().filter(m -> isQuantile.apply(0.75, m)).findFirst().get().value;
		this.loQ = samples.stream().filter(m -> isQuantile.apply(0.25, m)).findFirst().get().value;
		this.min = samples.stream().filter(m -> isQuantile.apply(Double.MIN_VALUE, m)).findFirst().get().value;
		this.max = samples.stream().filter(m -> isQuantile.apply(1.0, m)).findFirst().get().value;
	}

	public Snapshot(final List<Snapshot> snapshots) {
		double aMean;
		double aCount = 0;
		double aSum = 0;
		double aMed = 0; //?
		double aHiQ = 0; //?
		double aLoQ = 0; //?
		double aMin;
		double aMax;
		Snapshot tmp;
		aMin = ((tmp = snapshots.parallelStream().min(Comparator.comparing(Snapshot::min)).orElse(null)) != null)
			   ? tmp.min() : 0;
		aMax = ((tmp = snapshots.parallelStream().max(Comparator.comparing(Snapshot::max)).orElse(null)) != null)
			   ? tmp.max() : 0;
		for(Snapshot s : snapshots) {
			aSum += s.sum();
			aCount += s.count();
		}
		aMean = aSum / aCount;
		this.mean = aMean;
		this.count = aCount;
		this.sum = aSum;
		this.med = aMed;
		this.hiQ = aHiQ;
		this.loQ = aLoQ;
		this.min = aMin;
		this.max = aMax;
	}

	public double min() {
		return min;
	}

	public double max() {
		return max;
	}

	public double mean() {
		return mean;
	}

	public double count() {
		return count;
	}

	public double sum() {
		return sum;
	}

	public double med() {
		return med;
	}

	public double hiQ() {
		return hiQ;
	}

	public double loQ() {
		return loQ;
	}
}
