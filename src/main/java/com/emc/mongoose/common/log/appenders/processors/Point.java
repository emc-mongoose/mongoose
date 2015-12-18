package com.emc.mongoose.common.log.appenders.processors;

class Point {
	private double x;
	private double y;

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

	public double distance(Point point) {
		double dx = x - point.x;
		double dy = y - point.y;
		return Math.sqrt(dx * dx + dy * dy);
	}
}
