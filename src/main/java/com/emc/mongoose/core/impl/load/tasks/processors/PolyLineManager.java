package com.emc.mongoose.core.impl.load.tasks.processors;

import com.emc.mongoose.core.api.load.model.metrics.IoStats;

import java.util.List;

public final class PolyLineManager {

	private final static int MAX_NUM_OF_POINTS = 1000;
	private final static double BYTES_PER_MBYTE = 1048576;

	private final PolyLine
			durMin, durMax, durAvg,
			latMin, latMax, latAvg,
			tpAvg, tpLast,
			bwAvg, bwLast;

	private final long startTime;


	public PolyLineManager() {
		durMin = new PolyLine();
		durMax = new PolyLine();
		durAvg = new PolyLine();
		latMin = new PolyLine();
		latMax = new PolyLine();
		latAvg = new PolyLine();
		tpAvg = new PolyLine();
		tpLast = new PolyLine();
		bwAvg = new PolyLine();
		bwLast = new PolyLine();
		startTime = System.currentTimeMillis();
	}

	public final void updatePolylines(final IoStats.Snapshot metricsSnapshot) {
		addPoint(durMin, metricsSnapshot.getDurationMin());
		addPoint(durMax, metricsSnapshot.getDurationMax());
		addPoint(durAvg, metricsSnapshot.getDurationAvg());
		addPoint(latMin, metricsSnapshot.getLatencyMin());
		addPoint(latMax, metricsSnapshot.getLatencyMax());
		addPoint(latAvg, metricsSnapshot.getLatencyAvg());
		addPoint(tpAvg, metricsSnapshot.getSuccRateMean());
		addPoint(tpLast, metricsSnapshot.getSuccRateLast());
		addPoint(bwAvg, metricsSnapshot.getByteRateMean() / BYTES_PER_MBYTE);
		addPoint(bwLast, metricsSnapshot.getByteRateLast() / BYTES_PER_MBYTE);
	}

	private void addPoint(final PolyLine polyLine, final double metricValue) {
		double now = new Long((System.currentTimeMillis() - startTime) / 1000).doubleValue();
		if (polyLine.numberOfPoints() < MAX_NUM_OF_POINTS) {
			polyLine.addPoint(new Point(now, metricValue));
		} else {
			polyLine.simplify(MAX_NUM_OF_POINTS / 2);
			polyLine.addPoint(new Point(now, metricValue));
		}
	}

	final List<Point> getBwLast() {
		return bwLast.getPoints();
	}

	final List<Point> getBwAvg() {
		return bwAvg.getPoints();
	}

	final List<Point> getTpLast() {
		return tpLast.getPoints();
	}

	final List<Point> getTpAvg() {
		return tpAvg.getPoints();
	}

	final List<Point> getLatAvg() {
		return latAvg.getPoints();
	}

	final List<Point> getLatMax() {
		return latMax.getPoints();
	}

	final List<Point> getLatMin() {
		return latMin.getPoints();
	}

	final List<Point> getDurMax() {
		return durMax.getPoints();
	}

	final List<Point> getDurAvg() {
		return durAvg.getPoints();
	}

	final List<Point> getDurMin() {
		return durMin.getPoints();
	}
}
