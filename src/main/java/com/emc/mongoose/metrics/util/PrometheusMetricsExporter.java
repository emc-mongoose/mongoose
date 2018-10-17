package com.emc.mongoose.metrics.util;

import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.metrics.DistributedMetricsSnapshot;
import com.emc.mongoose.metrics.context.DistributedMetricsContext;

import io.prometheus.client.Collector;
import static io.prometheus.client.Collector.MetricFamilySamples.Sample;

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
				"The number of label names(" + names.length + ") does not match the number of values(" + values.length
					+ ")"
			);
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
	public List<MetricFamilySamples> collect() {
		final List<MetricFamilySamples> mfsList = new ArrayList<>();
		final DistributedMetricsSnapshot snapshot = metricsContext.lastSnapshot();
		if(snapshot != null) {
			collectSnapshot(snapshot.durationSnapshot(), mfsList);
			collectSnapshot(snapshot.latencySnapshot(), mfsList);
			collectSnapshot(snapshot.concurrencySnapshot(), mfsList);
			collectSnapshot(snapshot.byteSnapshot(), mfsList);
			collectSnapshot(snapshot.successSnapshot(), mfsList);
			collectSnapshot(snapshot.failsSnapshot(), mfsList);
		}
		return mfsList;
	}

	private void collectSnapshot(
		final SingleMetricSnapshot snapshot, final List<MetricFamilySamples> mfsList
	) {
		final List<Sample> samples = new ArrayList<>();
		if(snapshot instanceof TimingMetricSnapshot) {
			samples.addAll(collect((TimingMetricSnapshot) snapshot));
		} else if(snapshot instanceof RateMetricSnapshot){
			samples.addAll(collect((RateMetricSnapshot) snapshot));
		} else {
			Loggers.ERR.warn("Unexpected metric snapshot type: {}", snapshot.getClass());
		}
		final MetricFamilySamples mfs = new MetricFamilySamples(snapshot.name(), Type.GAUGE, help, samples);
		mfsList.add(mfs);
	}

	private List<Sample> collect(final RateMetricSnapshot metric) {
		final String metricName = metric.name();
		final List<Sample> samples = new ArrayList<>();
		samples.add(new Sample(metricName + "_count", labelNames, labelValues, metric.count()));
		samples.add(new Sample(metricName + "_meanRate", labelNames, labelValues, metric.mean()));
		samples.add(new Sample(metricName + "_lastRate", labelNames, labelValues, metric.last()));
		return samples;
	}

	private List<Sample> collect(final TimingMetricSnapshot metric) {
		final List<Sample> samples = new ArrayList<>();
		final HistogramSnapshot snapshot = metric.histogramSnapshot(); //for quantiles
		final String metricName = metric.name();
		samples.add(new Sample(metricName + "_count", labelNames, labelValues, metric.count()));
		samples.add(new Sample(metricName + "_sum", labelNames, labelValues, metric.sum()));
		samples.add(new Sample(metricName + "_mean", labelNames, labelValues, metric.mean()));
		samples.add(new Sample(metricName + "_min", labelNames, labelValues, metric.min()));
		for(int i = 0; i < quantileValues.size(); ++ i) {
			final Sample sample = new Sample(
				metricName + "_quantile_" + quantileValues.get(i), labelNames, labelValues,
				snapshot.quantile(quantileValues.get(i))
			);
			samples.add(sample);
		}
		samples.add(new Sample(metricName + "_max", labelNames, labelValues, metric.max()));
		return samples;
	}
}
