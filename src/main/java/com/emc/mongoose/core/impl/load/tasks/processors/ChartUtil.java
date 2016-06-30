package com.emc.mongoose.core.impl.load.tasks.processors;

import com.emc.mongoose.core.api.load.model.metrics.IoStats;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChartUtil {

	public static final Map<String, Map<String, Map<String, List<Metric>>>>
		CHARTS_MAP = new ConcurrentHashMap<>();

	public static void addChart(final String runId, final String loadJobName,
	                            final BasicPolylineManager basicManager
	) {

		final Map<String, List<Metric>> loadJobCharts = new ConcurrentHashMap<>();
		if (basicManager instanceof PolylineManager) {
			final PolylineManager manager = (PolylineManager) basicManager;
			loadJobCharts.put(IoStats.METRIC_NAME_LAT, Metric.latencyMetrics(manager));
			loadJobCharts.put(IoStats.METRIC_NAME_DUR, Metric.durationMetrics(manager));
			loadJobCharts.put(IoStats.METRIC_NAME_TP, Metric.throughputMetrics(manager));
			loadJobCharts.put(IoStats.METRIC_NAME_BW, Metric.bandwidthMetrics(manager));
		} else {
			loadJobCharts.put(IoStats.METRIC_NAME_LAT, Metric.latencyMetrics(basicManager));
			loadJobCharts.put(IoStats.METRIC_NAME_DUR, Metric.durationMetrics(basicManager));
			loadJobCharts.put(IoStats.METRIC_NAME_TP, Metric.throughputMetrics(basicManager));
			loadJobCharts.put(IoStats.METRIC_NAME_BW, Metric.bandwidthMetrics(basicManager));
		}
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
