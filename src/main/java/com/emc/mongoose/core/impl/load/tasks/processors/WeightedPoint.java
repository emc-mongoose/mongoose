package com.emc.mongoose.core.impl.load.tasks.processors;

class WeightedPoint implements Comparable<WeightedPoint> {

	private Point point;
	private double weight;

	public WeightedPoint(final Point point, final double weight) {
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
	public int compareTo(@SuppressWarnings("NullableProblems") WeightedPoint weightedPoint) {
		return Double.compare(weight, weightedPoint.weight);
	}

	@Override
	public String toString() {
		return point.toString() + " - " + weight;
	}
}
