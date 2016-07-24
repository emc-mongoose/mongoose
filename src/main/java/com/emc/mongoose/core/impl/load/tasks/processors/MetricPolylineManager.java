package com.emc.mongoose.core.impl.load.tasks.processors;

import com.emc.mongoose.core.api.load.model.metrics.IoStats;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public final class MetricPolylineManager
	extends BasicPolylineManager {

	public static final Map<String, MetricPolylineManager> MANAGERS = new LinkedHashMap<>();
	private final static int MAX_NUM_OF_POINTS = 1000;

	private final Polyline
		durMin, durMax,
		latMin, latMax,
		tpLast, bwLast;

	private final long startTime;


	public MetricPolylineManager() {
		durMin = new Polyline();
		durMax = new Polyline();
		latMin = new Polyline();
		latMax = new Polyline();
		tpLast = new Polyline();
		bwLast = new Polyline();
		startTime = System.currentTimeMillis();
	}

	public final void updatePolylines(final IoStats.Snapshot metricsSnapshot) {
		synchronized(System.out) {
			try {
				System.out.print(0);
				addPoint(durMin, metricsSnapshot.getDurationMin());
				System.out.print(1);
				addPoint(durMax, metricsSnapshot.getDurationMax());
				System.out.print(2);
				addPoint(durAvg(), metricsSnapshot.getDurationAvg());
				System.out.print(3);
				addPoint(latMin, metricsSnapshot.getLatencyMin());
				System.out.print(4);
				addPoint(latMax, metricsSnapshot.getLatencyMax());
				System.out.print(5);
				addPoint(latAvg(), metricsSnapshot.getLatencyAvg());
				System.out.print(6);
				addPoint(tpAvg(), metricsSnapshot.getSuccRateMean());
				System.out.print(7);
				addPoint(tpLast, metricsSnapshot.getSuccRateLast());
				System.out.print(8);
				addPoint(bwAvg(), metricsSnapshot.getByteRateMean() / BYTES_PER_MBYTE);
				System.out.print(9);
				addPoint(bwLast, metricsSnapshot.getByteRateLast() / BYTES_PER_MBYTE);
				System.out.println("~");
			} catch(final Throwable e) {
				e.printStackTrace(System.out);
			}
		}
	}


	private void addPoint(final Polyline polyline, final double metricValue) {
		final double now = new Long((System.currentTimeMillis() - startTime) / 1000).doubleValue();
		if(polyline.numberOfPoints() < MAX_NUM_OF_POINTS) {
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
