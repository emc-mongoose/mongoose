package com.emc.mongoose.metrics.util;

import com.emc.mongoose.Constants;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.metrics.context.DistributedMetricsContext;
import com.emc.mongoose.metrics.snapshot.ConcurrencyMetricSnapshot;
import com.emc.mongoose.metrics.snapshot.DistributedAllMetricsSnapshot;
import com.emc.mongoose.metrics.snapshot.HistogramSnapshot;
import com.emc.mongoose.metrics.snapshot.NamedMetricSnapshot;
import com.emc.mongoose.metrics.snapshot.RateMetricSnapshot;
import com.emc.mongoose.metrics.snapshot.TimingMetricSnapshot;
import io.prometheus.client.Collector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.emc.mongoose.metrics.MetricsConstants.METRIC_FORMAT;
import static com.emc.mongoose.metrics.MetricsConstants.METRIC_NAME_TIME;
import static io.prometheus.client.Collector.MetricFamilySamples.Sample;

/**
 @author veronika K. on 10.10.18 */
public class PrometheusMetricsExporterImpl
	extends Collector
	implements PrometheusMetricsExporter {

	private final List<String> labelValues = new ArrayList<>();
	private final List<String> labelNames = new ArrayList<>();
	private final DistributedMetricsContext metricsContext;
	private final List<Double> quantileValues = new ArrayList<>();
	private String help = "";

	public PrometheusMetricsExporterImpl(final DistributedMetricsContext context) {
		this.metricsContext = context;
	}

	@Override
	public PrometheusMetricsExporterImpl quantile(final double value) {
		if(value < 1.0 && value >= 0.0) {
			quantileValues.add(value);
		} else {
			throw new IllegalArgumentException("Invalid quantiele value : " + value);
		}
		return this;
	}

	@Override
	public PrometheusMetricsExporterImpl quantiles(final double[] values) {
		for(int i = 0; i < values.length; ++ i) {
			quantile(values[i]);
		}
		return this;
	}

	@Override
	public PrometheusMetricsExporterImpl quantiles(final List<Double> values) {
		for(int i = 0; i < values.size(); ++ i) {
			quantile(values.get(i));
		}
		return this;
	}

	@Override
	public PrometheusMetricsExporterImpl label(final String name, final String value) {
		this.labelNames.add(name);
		this.labelValues.add(value);
		return this;
	}

	@Override
	public PrometheusMetricsExporterImpl labels(final String[] names, final String[] values) {
		if(names.length != values.length) {
			throw new IllegalArgumentException(
				"The number of label names("
					+ names.length
					+ ") does not match the number of values("
					+ values.length
					+ ")");
		}
		this.labelNames.addAll(Arrays.asList(names));
		this.labelValues.addAll(Arrays.asList(values));
		return this;
	}

	@Override
	public PrometheusMetricsExporterImpl help(final String helpInfo) {
		help = helpInfo;
		return this;
	}

	@Override
	public List<MetricFamilySamples> collect() {
		final List<MetricFamilySamples> mfsList = new ArrayList<>();
		final DistributedAllMetricsSnapshot snapshot = metricsContext.lastSnapshot();
		if(snapshot != null) {
			collectSnapshot(snapshot.durationSnapshot(), mfsList);
			collectSnapshot(snapshot.latencySnapshot(), mfsList);
			collectSnapshot(snapshot.concurrencySnapshot(), mfsList);
			collectSnapshot(snapshot.byteSnapshot(), mfsList);
			collectSnapshot(snapshot.successSnapshot(), mfsList);
			collectSnapshot(snapshot.failsSnapshot(), mfsList);
			collectElapsedTime(snapshot.elapsedTimeMillis(), mfsList);
		}
		return mfsList;
	}

	private void collectElapsedTime(
		final double timeMillis, final List<MetricFamilySamples> mfsList
	) {
		mfsList.add(
			new MetricFamilySamples(
				"" + this.hashCode(),
				Type.GAUGE,
				help,
				Collections.singletonList(
					newSample(METRIC_NAME_TIME, "value", timeMillis / Constants.K))
			));
	}

	private void collectSnapshot(
		final NamedMetricSnapshot snapshot, final List<MetricFamilySamples> mfsList
	) {
		final List<Sample> samples = new ArrayList<>();
		if(snapshot instanceof TimingMetricSnapshot) {
			samples.addAll(collect((TimingMetricSnapshot) snapshot));
		} else if(snapshot instanceof RateMetricSnapshot) {
			samples.addAll(collect((RateMetricSnapshot) snapshot));
		} else if(snapshot instanceof ConcurrencyMetricSnapshot) {
			samples.addAll(collect((ConcurrencyMetricSnapshot) snapshot));
		} else {
			Loggers.ERR.warn("Unexpected metric snapshot type: {}", snapshot.getClass());
		}
		final MetricFamilySamples mfs =
			new MetricFamilySamples("" + this.hashCode(), Type.GAUGE, help, samples);
		mfsList.add(mfs);
	}

	private List<Sample> collect(final RateMetricSnapshot metric) {
		final String metricName = metric.name();
		final List<Sample> samples = new ArrayList<>();
		samples.add(newSample(metricName, "count", metric.count()));
		samples.add(newSample(metricName, "rate_mean", metric.mean()));
		samples.add(newSample(metricName, "rate_last", metric.last()));
		return samples;
	}

	private List<Sample> collect(final TimingMetricSnapshot metric) {
		final List<Sample> samples = new ArrayList<>();
		final HistogramSnapshot snapshot = metric.histogramSnapshot(); // for quantiles
		final String metricName = metric.name();
		samples.add(newSample(metricName, "count", metric.count()));
		samples.add(newSample(metricName, "sum", metric.sum() / Constants.M));
		samples.add(newSample(metricName, "mean", metric.mean() / Constants.M));
		samples.add(newSample(metricName, "min", metric.min() / Constants.M));
		for(int i = 0; i < quantileValues.size(); ++ i) {
			samples.add(
				newSample(
					metricName,
					"quantile_" + quantileValues.get(i),
					snapshot.quantile(quantileValues.get(i)) / Constants.M
				));
		}
		samples.add(newSample(metricName, "max", metric.max() / Constants.M));
		return samples;
	}

	private List<Sample> collect(final ConcurrencyMetricSnapshot metric) {
		final String metricName = metric.name();
		final List<Sample> samples = new ArrayList<>();
		samples.add(newSample(metricName, "mean", metric.mean()));
		samples.add(newSample(metricName, "last", metric.last()));
		return samples;
	}

	private Sample newSample(
		final String metricName, final String aggregationType, final double value
	) {
		return new Sample(
			String.format(METRIC_FORMAT, metricName, aggregationType), labelNames, labelValues, value);
	}
}
