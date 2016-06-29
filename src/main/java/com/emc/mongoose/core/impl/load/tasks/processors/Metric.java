package com.emc.mongoose.core.impl.load.tasks.processors;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public final class Metric
	implements Serializable {

	private final String name;
	private final List<Point> values;

	private Metric(final String name, final List<Point> values) {
		this.name = name;
		this.values = values;
	}

	private static List<Metric> timeMetricFormat(
		final List<Point> avgValues, final List<Point> minValues, final List<Point> maxValues
	) {
		final List<Metric> metrics = new LinkedList<>();
		metrics.add(new Metric("avg", avgValues));
		metrics.add(new Metric("min", minValues));
		metrics.add(new Metric("max", maxValues));
		return metrics;
	}

	private static List<Metric> speedMetricFormat(
		final List<Point> avgValues, final List<Point> lastValues
	) {
		final List<Metric> metrics = new LinkedList<>();
		metrics.add(new Metric("avg", avgValues));
		metrics.add(new Metric("last", lastValues));
		return metrics;
	}

	private static List<Metric> avgMetricFormat(final List<Point> avgValues) {
		final List<Metric> metrics = new LinkedList<>();
		metrics.add(new Metric("avg", avgValues));
		return metrics;
	}

	static List<Metric> latencyMetrics(final PolylineManager polylineManager) {
		return timeMetricFormat(polylineManager.getLatAvgs(), polylineManager.getLatMins(),
			polylineManager.getLatMaxs()
		);
	}

	static List<Metric> latencyMetrics(final BasicPolylineManager polylineManager) {
		return avgMetricFormat(polylineManager.getLatAvgs());
	}

	static List<Metric> durationMetrics(final PolylineManager polylineManager) {
		return timeMetricFormat(polylineManager.getDurAvgs(), polylineManager.getDurMins(),
			polylineManager.getDurMaxs()
		);
	}

	static List<Metric> durationMetrics(final BasicPolylineManager polylineManager) {
		return avgMetricFormat(polylineManager.getDurAvgs());

	}

	static List<Metric> throughputMetrics(final PolylineManager polylineManager) {
		return speedMetricFormat(polylineManager.getTpAvgs(), polylineManager.getTpLasts());
	}

	static List<Metric> throughputMetrics(final BasicPolylineManager polylineManager) {
		return avgMetricFormat(polylineManager.getTpAvgs());
	}

	static List<Metric> bandwidthMetrics(final PolylineManager polylineManager) {
		return speedMetricFormat(polylineManager.getBwAvgs(), polylineManager.getBwLasts());
	}

	static List<Metric> bandwidthMetrics(final BasicPolylineManager polylineManager) {
		return avgMetricFormat(polylineManager.getBwAvgs());
	}
}
