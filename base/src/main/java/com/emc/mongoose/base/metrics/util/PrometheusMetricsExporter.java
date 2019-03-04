package com.emc.mongoose.base.metrics.util;

import java.util.List;

/** @author veronika K. on 10.10.18 */
public interface PrometheusMetricsExporter {

	PrometheusMetricsExporter quantile(final double value);

	PrometheusMetricsExporter quantiles(final double[] values);

	PrometheusMetricsExporter quantiles(final List<Double> values);

	PrometheusMetricsExporter label(final String name, final String value);

	PrometheusMetricsExporter labels(final String[] names, final String[] values);

	PrometheusMetricsExporter help(final String helpInfo);
}
