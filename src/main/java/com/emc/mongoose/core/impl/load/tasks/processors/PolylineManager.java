package com.emc.mongoose.core.impl.load.tasks.processors;

import com.emc.mongoose.core.api.load.model.metrics.IOStats;

import java.util.Map;

public class PolylineManager {

//	Map<String, Polyline> polylines;
//
//	public PolylineManager(IOStats.Snapshot metricsSnapshot) {
//
//	}

	private final static int MAX_NUM_OF_POINTS = 4;

	private Polyline polyline = new Polyline();


	public void addPoint(Point newLastPoint) {
		System.out.println(polyline);
		if (polyline.numberOfPoints() < MAX_NUM_OF_POINTS) {
			polyline.addPoint(newLastPoint);
		} else {
			polyline.simplify(MAX_NUM_OF_POINTS / 2);
			polyline.addPoint(newLastPoint);
		}
	}

}
