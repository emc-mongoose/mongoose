package com.emc.mongoose.core.impl.load.tasks.processors;

final class WeightedPoint implements Comparable<WeightedPoint> {

	private final Point point;
	private double weight;

	WeightedPoint(final Point point, final double weight) {
		this.point = point;
		this.weight = weight;
	}

	public final Point point() {
		return point;
	}

	public final void setWeight(final double weight) {
		this.weight = weight;
	}

	@Override
	public int compareTo(@SuppressWarnings("NullableProblems") WeightedPoint weightedPoint) {
		return Double.compare(weight, weightedPoint.weight);
	}

	@Override
	public final String toString() {
		return point.toString() + " - " + weight;
	}
}
