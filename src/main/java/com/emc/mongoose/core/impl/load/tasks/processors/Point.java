package com.emc.mongoose.core.impl.load.tasks.processors;

import java.io.Serializable;

import static java.lang.Math.sqrt;

class Point implements Serializable {
	private double x;
	private double y;

	public Point(final double x, final double y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

	public static double distance(final Point point1, final Point point2) {
		double dx = point1.x - point2.x;
		double dy = point1.y - point2.y;
		return sqrt(dx * dx + dy * dy);
	}
}
