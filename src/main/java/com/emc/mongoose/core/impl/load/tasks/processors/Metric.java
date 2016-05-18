package com.emc.mongoose.core.impl.load.tasks.processors;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class Metric implements Serializable {

	private String name;
	private List<Point> values;

	private Metric(final String name, final List<Point> values) {
		this.name = name;
		this.values = values;
	}

	private static List<Metric> timeMetricFormat(final List<Point> avgValues,
	                                            final List<Point> minValues,
	                                            final List<Point> maxValues) {
		final List<Metric> metrics = new LinkedList<>();
		metrics.add(new Metric("avg", avgValues));
		metrics.add(new Metric("min", minValues));
		metrics.add(new Metric("max", maxValues));
		return metrics;
	}

	private static List<Metric> speedMetricFormat(final List<Point> avgValues,
	                                             final List<Point> lastValues) {
		final List<Metric> metrics = new LinkedList<>();
		metrics.add(new Metric("avg", avgValues));
		metrics.add(new Metric("last", lastValues));
		return metrics;
	}

	public static List<Metric> latencyMetrics(final PolylineManager polylineManager) {
		return timeMetricFormat(
				polylineManager.getLatAvg(),
				polylineManager.getLatMin(),
				polylineManager.getLatMax());
	}

	public static List<Metric> durationMetrics(final PolylineManager polylineManager) {
		return timeMetricFormat(
				polylineManager.getDurAvg(),
				polylineManager.getDurMin(),
				polylineManager.getDurMax());
	}

	public static List<Metric> throughputMetrics(final PolylineManager polylineManager) {
		return speedMetricFormat(
				polylineManager.getTpAvg(),
				polylineManager.getTpLast());
	}

	public static List<Metric> bandwidthMetrics(final PolylineManager polylineManager) {
		return speedMetricFormat(
				polylineManager.getBwAvg(),
				polylineManager.getBwLast());
	}
}
