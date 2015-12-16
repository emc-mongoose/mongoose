package com.emc.mongoose.common.log.appenders.processors;

import java.util.Comparator;

/**
 * Created by ilya on 16.12.15.
 */
public class WeighedPoint implements Comparable<WeighedPoint> {

	private Point point;
	private double weight;

	public WeighedPoint(Point point) {
		this.point = point;
		weight = 0.0;
	}

	public WeighedPoint(Point point, double weight) {
		this.point = point;
		this.weight = weight;
	}

	public Point point() {
		return point;
	}

	public double weight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	@Override
	public int compareTo(WeighedPoint weighedPoint) {
		return Double.compare(weight, weighedPoint.weight);
	}

	public final static class WeightComparator implements Comparator<WeighedPoint> {

		public WeightComparator() {
		}

		@Override
		public int compare(WeighedPoint weighedPoint1, WeighedPoint weighedPoint2) {
			return Double.compare(weighedPoint1.weight, weighedPoint2.weight);
		}
	}
}
