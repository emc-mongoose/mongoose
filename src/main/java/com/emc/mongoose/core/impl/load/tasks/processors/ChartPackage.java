package com.emc.mongoose.core.impl.load.tasks.processors;

import java.io.Serializable;
import java.util.List;

public class ChartPackage implements Serializable {

	private String name;
	private String runId;
	private String loadJobName;
	private List<Metric> duration, latency, thoughput, bandwidth;

	public ChartPackage(String runId, String loadJobName, PolylineManager polylineManager) {
		this.name = "chrtpckg";
		this.runId = runId;
		this.loadJobName = loadJobName;
		this.duration = Metric.timeMetricFormat(
				polylineManager.getDurAvg(),
				polylineManager.getDurMin(),
				polylineManager.getDurMax());
		this.latency = Metric.timeMetricFormat(
				polylineManager.getLatAvg(),
				polylineManager.getLatMin(),
				polylineManager.getLatMax());
		this.thoughput = Metric.speedMetricFormat(
				polylineManager.getTpAvg(),
				polylineManager.getTpLast());
		this.bandwidth = Metric.speedMetricFormat(
				polylineManager.getBwAvg(),
				polylineManager.getBwLast());
	}
}
