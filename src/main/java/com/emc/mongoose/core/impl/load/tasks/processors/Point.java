package com.emc.mongoose.core.impl.load.tasks.processors;

import static java.lang.Math.sqrt;

import java.io.Serializable;

class Point implements Serializable {
	double x;
	double y;

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

	public static double distance(Point point1, Point point2) {
		double dx = point1.x - point2.x;
		double dy = point1.y - point2.y;
		return sqrt(dx * dx + dy * dy);
	}
}
