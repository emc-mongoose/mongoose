package com.emc.mongoose.core.impl.load.tasks.processors;

import com.emc.mongoose.core.api.load.model.metrics.IOStats;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChartPackage  {

	public static final Map<String, Map<String, Map<String, List<Metric>>>>
		CHARTS_MAP = new ConcurrentHashMap<>();
	static {

	}

	public static void addChart(final String runId, final String loadJobName,
	                            final PolyLineManager polyLineManager
	) {
		final Map<String, List<Metric>> loadJobCharts = new ConcurrentHashMap<>();
		loadJobCharts.put(IOStats.METRIC_NAME_LAT, Metric.latencyMetrics(polyLineManager));
		loadJobCharts.put(IOStats.METRIC_NAME_DUR, Metric.durationMetrics(polyLineManager));
		loadJobCharts.put(IOStats.METRIC_NAME_TP, Metric.throughputMetrics(polyLineManager));
		loadJobCharts.put(IOStats.METRIC_NAME_BW, Metric.bandwidthMetrics(polyLineManager));
		if(!CHARTS_MAP.containsKey(runId)) {
			final Map<String, Map<String, List<Metric>>> runIdCharts = new ConcurrentHashMap<>();
			runIdCharts.put(loadJobName, loadJobCharts);
			CHARTS_MAP.put(runId, runIdCharts);
		} else {
			CHARTS_MAP.get(runId).put(loadJobName, loadJobCharts);
		}
	}

	public static Map<String, Map<String, List<Metric>>> getChart(final String runId) {
		return CHARTS_MAP.get(runId);
	}

}
