package com.emc.mongoose.core.impl.load.tasks.processors;

public final class WeighedPoint
implements Comparable<WeighedPoint> {

	private final Point point;

	private double weight;

	public WeighedPoint(final Point point, final double weight) {
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
	public final int compareTo(@SuppressWarnings("NullableProblems") WeighedPoint weighedPoint) {
		return Double.compare(weight, weighedPoint.weight);
	}

	@Override
	public final String toString() {
		return point.toString() + " - " + weight;
	}
}
