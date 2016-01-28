package com.emc.mongoose.core.impl.load.tasks.processors;

import java.io.Serializable;
import java.util.List;

public class ChartPackage implements Serializable {

	String runId;
	String loadJobName;
	List<Point> durMinChart, durMaxChart, durAvgChart,
			latMinChart, latMaxChart, latAvgChart,
			tpAvgChart, tpLastChart,
			bwAvgChart, bwLast;

	public ChartPackage(String runId, String loadJobName, PolylineManager polylineManager) {
		this.runId = runId;
		this.loadJobName = loadJobName;
		this.durMinChart = polylineManager.getDurMin();
		this.durMaxChart = polylineManager.getDurMax();
		this.durAvgChart = polylineManager.getDurAvg();
		this.latMinChart = polylineManager.getLatMin();
		this.latMaxChart = polylineManager.getLatMax();
		this.latAvgChart = polylineManager.getLatAvg();
		this.tpAvgChart = polylineManager.getTpAvg();
		this.tpLastChart = polylineManager.getTpLast();
		this.bwAvgChart = polylineManager.getBwAvg();
		this.bwLast = polylineManager.getBwLast();
	}
}
