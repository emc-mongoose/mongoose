package com.emc.mongoose.core.impl.load.tasks.processors;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public final class Metric implements Serializable {

	private final String name;
	private final List<Point> values;

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

	static List<Metric> latencyMetrics(final PolyLineManager polyLineManager) {
		return timeMetricFormat(
				polyLineManager.getLatAvg(),
				polyLineManager.getLatMin(),
				polyLineManager.getLatMax());
	}

	static List<Metric> durationMetrics(final PolyLineManager polyLineManager) {
		return timeMetricFormat(
				polyLineManager.getDurAvg(),
				polyLineManager.getDurMin(),
				polyLineManager.getDurMax());
	}

	static List<Metric> throughputMetrics(final PolyLineManager polyLineManager) {
		return speedMetricFormat(
				polyLineManager.getTpAvg(),
				polyLineManager.getTpLast());
	}

	static List<Metric> bandwidthMetrics(final PolyLineManager polyLineManager) {
		return speedMetricFormat(
				polyLineManager.getBwAvg(),
				polyLineManager.getBwLast());
	}
}
