package com.emc.mongoose.core.impl.load.tasks.processors;

import com.emc.mongoose.core.api.load.model.metrics.IoStats;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public final class PolylineManager extends BasicPolylineManager {

	private final static int MAX_NUM_OF_POINTS = 1000;


	private final Polyline
		durMin, durMax,
		latMin, latMax,
		tpLast, bwLast;

	private final long startTime;


	public PolylineManager() {
		durMin = new Polyline();
		durMax = new Polyline();
		latMin = new Polyline();
		latMax = new Polyline();
		tpLast = new Polyline();
		bwLast = new Polyline();
		startTime = System.currentTimeMillis();
	}

	public final void updatePolylines(final IoStats.Snapshot metricsSnapshot) {
		addPoint(durMin, metricsSnapshot.getDurationMin());
		addPoint(durMax, metricsSnapshot.getDurationMax());
		addPoint(durAvg(), metricsSnapshot.getDurationAvg());
		addPoint(latMin, metricsSnapshot.getLatencyMin());
		addPoint(latMax, metricsSnapshot.getLatencyMax());
		addPoint(latAvg(), metricsSnapshot.getLatencyAvg());
		addPoint(tpAvg(), metricsSnapshot.getSuccRateMean());
		addPoint(tpLast, metricsSnapshot.getSuccRateLast());
		addPoint(bwAvg(), metricsSnapshot.getByteRateMean() / BYTES_PER_MBYTE);
		addPoint(bwLast, metricsSnapshot.getByteRateLast() / BYTES_PER_MBYTE);
	}


	private void addPoint(final Polyline polyline, final double metricValue) {
		final double now = new Long((System.currentTimeMillis() - startTime) / 1000).doubleValue();
		if (polyline.numberOfPoints() < MAX_NUM_OF_POINTS) {
			polyline.addPoint(new Point(now, metricValue));
		} else {
			polyline.simplify(MAX_NUM_OF_POINTS / 2);
			polyline.addPoint(new Point(now, metricValue));
		}
	}

	public final List<Point> getBwLasts() {
		return bwLast.getPoints();
	}

	public final List<Point> getTpLasts() {
		return tpLast.getPoints();
	}

	public final List<Point> getLatMaxs() {
		return latMax.getPoints();
	}

	public final List<Point> getLatMins() {
		return latMin.getPoints();
	}

	public final List<Point> getDurMaxs() {
		return durMax.getPoints();
	}

	public final List<Point> getDurMins() {
		return durMin.getPoints();
	}
}
