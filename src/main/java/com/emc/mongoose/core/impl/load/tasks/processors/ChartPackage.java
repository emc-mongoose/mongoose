package com.emc.mongoose.core.impl.load.tasks.processors;

import com.emc.mongoose.core.api.load.model.metrics.IoStats;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChartPackage  {

	public static final Map<String, Map<String, Map<String, List<Metric>>>>
		CHARTS_MAP = new ConcurrentHashMap<>();

	public static void addChart(final String runId, final String loadJobName,
	                            final PolyLineManager polyLineManager
	) {
		final Map<String, List<Metric>> loadJobCharts = new ConcurrentHashMap<>();
		loadJobCharts.put(IoStats.METRIC_NAME_LAT, Metric.latencyMetrics(polyLineManager));
		loadJobCharts.put(IoStats.METRIC_NAME_DUR, Metric.durationMetrics(polyLineManager));
		loadJobCharts.put(IoStats.METRIC_NAME_TP, Metric.throughputMetrics(polyLineManager));
		loadJobCharts.put(IoStats.METRIC_NAME_BW, Metric.bandwidthMetrics(polyLineManager));
		if(!CHARTS_MAP.containsKey(runId)) {
			final Map<String, Map<String, List<Metric>>> runIdCharts = new ConcurrentHashMap<>();
			runIdCharts.put(loadJobName, loadJobCharts);
			CHARTS_MAP.put(runId, runIdCharts);
		}
		if(CHARTS_MAP.get(runId).containsKey(loadJobName)) {
			CHARTS_MAP.get(runId).put(loadJobName, loadJobCharts);
		}
	}

	public static List<Metric> getChart(final String runId, final String loadJobName,
	                                    final String metricName) {
		final Map<String, Map<String, List<Metric>>> runIdCharts = CHARTS_MAP.get(runId);
		if(runIdCharts != null) {
			final Map<String, List<Metric>>
				loadJobCharts = runIdCharts.get(runIdCharts.keySet().iterator().next());
			if(loadJobCharts != null) {
				return loadJobCharts.get(metricName);
			}
		}
		return null;
	}


}
