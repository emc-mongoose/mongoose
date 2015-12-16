package com.emc.mongoose.common.log.appenders.processors;

import java.util.Comparator;

class Point implements Comparable<Point>, Comparator<Point> {

	public enum Ordinates {
		X, Y
	}

	private double x;
	private double y;

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public Point() {
		this(0.0, 0.0);
	}

	public Point(Point point) {
		this(point.x, point.y);
	}

	public void setCoordinate(Point point) {
		x = point.x;
		y = point.y;
	}

	public double getOrdinate(Ordinates ordinate) {
		switch (ordinate) {
			case X: return x;
			case Y: return y;
		}
		throw new IllegalArgumentException(ordinate.name());
	}

	public void setOrdinate(Ordinates ordinate, double value)
	{
		switch (ordinate) {
			case X:
				x = value;
				break;
			case Y:
				y = value;
				break;
			default:
				throw new IllegalArgumentException(ordinate.name());
		}
	}

	public boolean equals(Object object) {
		return object instanceof Point && equals((Point) object);
	}

	private boolean equals(Point point) {
		if (x != point.x) {
			return false;
		}
		return y == point.y;
	}

	@Override
	public int compareTo(Point point) {
		return compare(this, point);
	}

	@Override
	public int compare(Point point1, Point point2) {
		int comparedX = Double.compare(point1.x, point2.x);
		if (comparedX == 0) {
			return Double.compare(point1.y, point2.y);
		} else {
			return comparedX;
		}
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

	public Point copy() {
		return new Point(this);
	}

	public double distance(Point point) {
		double dx = x - point.x;
		double dy = y - point.y;
		return Math.sqrt(dx * dx + dy * dy);
	}

	public static int hashCode(double x) {
		long f = Double.doubleToLongBits(x);
		return (int)(f^(f>>>32));
	}
}
