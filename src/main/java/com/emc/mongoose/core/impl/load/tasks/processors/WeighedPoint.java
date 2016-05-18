package com.emc.mongoose.core.impl.load.tasks.processors;

class WeighedPoint implements Comparable<WeighedPoint> {

	private Point point;
	private double weight;

	public WeighedPoint(final Point point, final double weight) {
		this.point = point;
		this.weight = weight;
	}

	public Point point() {
		return point;
	}

	public void setWeight(final double weight) {
		this.weight = weight;
	}

	@Override
	public int compareTo(@SuppressWarnings("NullableProblems") WeighedPoint weighedPoint) {
		return Double.compare(weight, weighedPoint.weight);
	}

	@Override
	public String toString() {
		return point.toString() + " - " + weight;
	}
}
