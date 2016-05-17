package com.emc.mongoose.core.impl.load.tasks.processors;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class Metric implements Serializable {

	private String name;
	private List<Point> values;

	private Metric(String name, List<Point> values) {
		this.name = name;
		this.values = values;
	}

	public static List<Metric> timeMetricFormat(List<Point> avgValues,
	                                            List<Point> minValues,
	                                            List<Point> maxValues) {
		List<Metric> metrics = new LinkedList<>();
		metrics.add(new Metric("avg", avgValues));
		metrics.add(new Metric("min", minValues));
		metrics.add(new Metric("max", maxValues));
		return metrics;
	}

	public static List<Metric> speedMetricFormat(List<Point> avgValues,
	                                             List<Point> lastValues) {
		List<Metric> metrics = new LinkedList<>();
		metrics.add(new Metric("avg", avgValues));
		metrics.add(new Metric("last", lastValues));
		return metrics;
	}
}
