package com.emc.mongoose.metrics.util;

import com.emc.mongoose.metrics.DistributedMetricsSnapshot;
import com.emc.mongoose.metrics.context.DistributedMetricsContext;
import io.prometheus.client.Collector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 @author veronika K. on 10.10.18 */
public class PrometheusMetricsExporter
extends Collector {

	private final List<String> labelValues = new ArrayList<>();
	private final List<String> labelNames = new ArrayList<>();
	private final DistributedMetricsContext metricsContext;
	private final List<Double> quantileValues = new ArrayList<>();
	private String help = "";

	public PrometheusMetricsExporter(final DistributedMetricsContext context) {
		this.metricsContext = context;
	}

	public PrometheusMetricsExporter quantile(final double value) {
		if(value <= 1.0 && value >= 0.0) {
			quantileValues.add(value);
		} else {
			throw new IllegalArgumentException("Invalid quantiele value : " + value);
		}
		return this;
	}

	public PrometheusMetricsExporter quantiles(final double[] values) {
		for(int i = 0; i < values.length; ++ i) {
			quantile(values[i]);
		}
		return this;
	}

	public PrometheusMetricsExporter label(final String name, final String value) {
		this.labelNames.add(name);
		this.labelValues.add(value);
		return this;
	}

	public PrometheusMetricsExporter labels(final String[] names, final String[] values) {
		if(names.length != values.length) {
			throw new IllegalArgumentException(
				"The number of label names(" + names.length +
					") does not match the number of values(" + values.length + ")");
		}
		this.labelNames.addAll(Arrays.asList(names));
		this.labelValues.addAll(Arrays.asList(values));
		return this;
	}

	public PrometheusMetricsExporter help(final String helpInfo) {
		help = helpInfo;
		return this;
	}

	@Override
	public List<Collector.MetricFamilySamples> collect() {
		final DistributedMetricsSnapshot metricsSnapshot = metricsContext.lastSnapshot();
		final List<SingleMetricSnapshot> metrics = new ArrayList<>(6);
		metrics.add(metricsSnapshot.durationSnapshot());
		metrics.add(metricsSnapshot.latencySnapshot());
		metrics.add(metricsSnapshot.concurrencySnapshot());
		metrics.add(metricsSnapshot.byteSnapshot());
		metrics.add(metricsSnapshot.successSnapshot());
		metrics.add(metricsSnapshot.failsSnapshot());
		//
		final List<Collector.MetricFamilySamples> mfsList = new ArrayList<>();
		for(final SingleMetricSnapshot metric : metrics) {
			final List<Collector.MetricFamilySamples.Sample> samples = new ArrayList<>();
			if(metric instanceof TimingMetricSnapshot) {
				samples.addAll(collect((TimingMetricSnapshot) metric));
			} else {
				samples.addAll(collect((RateMetricSnapshot) metric));
			}
			final MetricFamilySamples mfs = new MetricFamilySamples(metric.name(), Type.UNTYPED, help, samples);
			mfsList.add(mfs);
		}
		return mfsList;
	}

	private List<MetricFamilySamples.Sample> collect(final RateMetricSnapshot metric) {
		final String metricName = metric.name();
		final List<Collector.MetricFamilySamples.Sample> samples = new ArrayList<>();
		samples.add(new Collector.MetricFamilySamples.Sample(metricName + "_count", labelNames, labelValues,
			metric.count()
		));
		samples.add(new Collector.MetricFamilySamples.Sample(metricName + "_meanRate", labelNames, labelValues,
			metric.mean()
		));
		samples.add(new Collector.MetricFamilySamples.Sample(metricName + "_lastRate", labelNames, labelValues,
			metric.last()
		));
		return samples;
	}

	private List<MetricFamilySamples.Sample> collect(final TimingMetricSnapshot metric) {
		final List<Collector.MetricFamilySamples.Sample> samples = new ArrayList<>();
		final HistogramSnapshot snapshot = metric.histogramSnapshot(); //for quantieles
		final String metricName = metric.name();
		samples.add(new Collector.MetricFamilySamples.Sample(metricName + "_count", labelNames, labelValues,
			metric.count()
		));
		samples.add(new Collector.MetricFamilySamples.Sample(metricName + "_sum", labelNames, labelValues,
			metric.sum()
		));
		samples.add(
			new Collector.MetricFamilySamples.Sample(metricName + "_mean", labelNames, labelValues,
				metric.mean()
			));
		samples.add(
			new Collector.MetricFamilySamples.Sample(metricName + "_min", labelNames, labelValues,
				metric.min()
			));
		for(int i = 0; i < quantileValues.size(); ++ i) {
			samples.add(
				new Collector.MetricFamilySamples.Sample(metricName + "_quantile_" + quantileValues.get(i),
					labelNames, labelValues, snapshot.quantile(quantileValues.get(i))
				));
		}
		samples.add(
			new Collector.MetricFamilySamples.Sample(metricName + "_max", labelNames, labelValues,
				metric.max()
			));
		return samples;
	}
}
