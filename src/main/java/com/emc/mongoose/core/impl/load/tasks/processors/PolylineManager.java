package com.emc.mongoose.core.impl.load.tasks.processors;

import com.emc.mongoose.core.api.load.model.metrics.IOStats;

import java.util.List;

public class PolylineManager {

	private final static int MAX_NUM_OF_POINTS = 6;

	private Polyline
			durMin, durMax, durAvg,
			latMin, latMax, latAvg,
			tpAvg, tpLast,
			bwAvg, bwLast;

	private final long startTime;


	public PolylineManager() {
		durMin = new Polyline();
		durMax = new Polyline();
		durAvg = new Polyline();
		latMin = new Polyline();
		latMax = new Polyline();
		latAvg = new Polyline();
		tpAvg = new Polyline();
		tpLast = new Polyline();
		bwAvg = new Polyline();
		bwLast = new Polyline();
		startTime = System.currentTimeMillis();
	}

	public void updatePolylines(final IOStats.Snapshot metricsSnapshot) {
		addPoint(durMin, metricsSnapshot.getDurationMin());
		addPoint(durMax, metricsSnapshot.getDurationMax());
		addPoint(durAvg, metricsSnapshot.getDurationAvg());
		addPoint(latMin, metricsSnapshot.getLatencyMin());
		addPoint(latMax, metricsSnapshot.getLatencyMax());
		addPoint(latAvg, metricsSnapshot.getLatencyAvg());
		addPoint(tpAvg, metricsSnapshot.getSuccRateMean());
		addPoint(tpLast, metricsSnapshot.getSuccRateLast());
		addPoint(bwAvg, metricsSnapshot.getByteRateMean());
		addPoint(bwLast, metricsSnapshot.getByteRateLast());
	}

	private void addPoint(final Polyline polyline, final double metricValue) {
		double now = new Long((System.currentTimeMillis() - startTime) / 1000).doubleValue();
		if (polyline.numberOfPoints() < MAX_NUM_OF_POINTS) {
			polyline.addPoint(new Point(now, metricValue));
		} else {
			polyline.simplify(MAX_NUM_OF_POINTS / 2);
			polyline.addPoint(new Point(now, metricValue));
		}
	}

	public List<Point> getBwLast() {
		return bwLast.getPoints();
	}

	public List<Point> getBwAvg() {
		return bwAvg.getPoints();
	}

	public List<Point> getTpLast() {
		return tpLast.getPoints();
	}

	public List<Point> getTpAvg() {
		return tpAvg.getPoints();
	}

	public List<Point> getLatAvg() {
		return latAvg.getPoints();
	}

	public List<Point> getLatMax() {
		return latMax.getPoints();
	}

	public List<Point> getLatMin() {
		return latMin.getPoints();
	}

	public List<Point> getDurMax() {
		return durMax.getPoints();
	}

	public List<Point> getDurAvg() {
		return durAvg.getPoints();
	}

	public List<Point> getDurMin() {
		return durMin.getPoints();
	}
}
