package com.emc.mongoose.core.impl.load.tasks.processors;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public final class Metric
	implements Serializable {

	private final String name;
	private final List<Point> values;

	private Metric(final String name, final List<Point> values) {
		this.name = name;
		this.values = values;
	}

//	private static List<Metric> timeMetricFormat(
//		final List<Point> avgValues, final List<Point> minValues, final List<Point> maxValues
//	) {
//		final List<Metric> metrics = new LinkedList<>();
//		metrics.add(new Metric("avg", avgValues));
//		metrics.add(new Metric("min", minValues));
//		metrics.add(new Metric("max", maxValues));
//		return metrics;
//	}
//
//	private static List<Metric> speedMetricFormat(
//		final List<Point> avgValues, final List<Point> lastValues
//	) {
//		final List<Metric> metrics = new LinkedList<>();
//		metrics.add(new Metric("avg", avgValues));
//		metrics.add(new Metric("last", lastValues));
//		return metrics;
//	}
//
//	private static List<Metric> customMetricFormat(final String metricName, final List<Point> values) {
//		final List<Metric> metrics = new LinkedList<>();
//		metrics.add(new Metric(metricName, values));
//		return metrics;
//	}

	private static List<Metric> customMetricFormat(final Map<String, List<Point>> valuesByName) {
		final List<Metric> metrics = new LinkedList<>();
		for (final String name: valuesByName.keySet()) {
			metrics.add(new Metric(name, valuesByName.get(name)));
		}
		return metrics;
	}

	private static Map<String, List<Point>> speedValues(
		final List<Point> avgValues, final List<Point> lastValues) {
		final Map<String, List<Point>> speedValues = new LinkedHashMap<>();
		speedValues.put("avg", avgValues);
		speedValues.put("last", lastValues);
		return speedValues;
	}

	private static Map<String, List<Point>> timeValues(
		final List<Point> avgValues, final List<Point> minValues, final List<Point> maxValues) {
		final Map<String, List<Point>> timeValues = new LinkedHashMap<>();
		timeValues.put("avg", avgValues);
		timeValues.put("min", minValues);
		timeValues.put("max", maxValues);
		return timeValues;
	}

	static List<Metric> latencyMetrics(final MetricPolylineManager polylineManager) {
		return customMetricFormat(timeValues(polylineManager.getLatAvgs(), polylineManager.getLatMins(),
			polylineManager.getLatMaxs())
		);
	}

	static List<Metric> latencyMetrics(final Map<String, BasicPolylineManager> managers) {
		final Map<String, List<Point>> values = new LinkedHashMap<>();
		for (final String itemDataSize: managers.keySet()) {
			values.put(itemDataSize, managers.get(itemDataSize).getLatAvgs());
		}
		return customMetricFormat(values);
	}

	static List<Metric> durationMetrics(final MetricPolylineManager polylineManager) {
		return customMetricFormat(timeValues(polylineManager.getDurAvgs(), polylineManager.getDurMins(),
			polylineManager.getDurMaxs())
		);
	}

	static List<Metric> durationMetrics(final Map<String, BasicPolylineManager> managers) {
		final Map<String, List<Point>> values = new LinkedHashMap<>();
		for (final String itemDataSize: managers.keySet()) {
			values.put(itemDataSize, managers.get(itemDataSize).getDurAvgs());
		}
		return customMetricFormat(values);
	}

	static List<Metric> throughputMetrics(final MetricPolylineManager polylineManager) {
		return customMetricFormat(speedValues(polylineManager.getTpAvgs(), polylineManager.getTpLasts()));
	}

	static List<Metric> throughputMetrics(final Map<String, BasicPolylineManager> managers) {
		final Map<String, List<Point>> values = new LinkedHashMap<>();
		for (final String itemDataSize: managers.keySet()) {
			values.put(itemDataSize, managers.get(itemDataSize).getTpAvgs());
		}
		return customMetricFormat(values);
	}

	static List<Metric> bandwidthMetrics(final MetricPolylineManager polylineManager) {
		return customMetricFormat(speedValues(polylineManager.getBwAvgs(), polylineManager.getBwLasts()));
	}

	static List<Metric> bandwidthMetrics(final Map<String, BasicPolylineManager> managers) {
		final Map<String, List<Point>> values = new LinkedHashMap<>();
		for (final String itemDataSize: managers.keySet()) {
			values.put(itemDataSize, managers.get(itemDataSize).getBwAvgs());
		}
		return customMetricFormat(values);
	}
}
