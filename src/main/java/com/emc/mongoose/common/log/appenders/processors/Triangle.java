package com.emc.mongoose.common.log.appenders.processors;

import java.util.Comparator;

public class Triangle implements Comparable<Triangle>{

	private double a, b, c;
	private Point point1, point3;
	private WeighedPoint point2;
	private Triangle prev;
	private Triangle next;

	public Triangle(Point point1, WeighedPoint wPoint2, Point point3) {
		this.point1 = point1;
		this.point2 = wPoint2;
		this.point3 = point3;
		a = point1.distance(wPoint2.point());
		b = wPoint2.point().distance(point3);
		c = point3.distance(point1);
		double p = (a + b + c) / 2;
		point2.setWeight(Math.sqrt(p * (p - a) * (p - b) * (p - c)));
	}

	public void setPoint2(WeighedPoint middlePoint) {
		point2 = middlePoint;
	}

	public WeighedPoint point2() {
		return point2;
	}

	public void setMiddlePoint(WeighedPoint middlePoint) {
		point2 = middlePoint;
	}

	public WeighedPoint middlePoint() {
		return point2;
	}

	public Point getPoint1() {
		return point1;
	}

	public void setPoint1(Point point1) {
		this.point1 = point1;
	}

	public Point getPoint3() {
		return point3;
	}

	public void setPoint3(Point point3) {
		this.point3 = point3;
	}

	public double weight() {
		return point2.weight();
	}

	public void setWeight(double weight) {
		point2.setWeight(weight);
	}

	public Triangle prev() {
		return prev;
	}

	public void setPrev(Triangle prev) {
		this.prev = prev;
	}

	public Triangle next() {
		return next;
	}

	public void setNext(Triangle next) {
		this.next = next;
	}

	public double area() {
		double p = (a + b + c) / 2;
		return Math.sqrt(p * (p - a) * (p - b) * (p - c));
	}

	@Override
	public int compareTo(Triangle triangle) {
		return Double.compare(area(), triangle.area());
	}

	public final static class AreaComparator implements Comparator<Triangle> {

		@Override
		public int compare(Triangle triangle1, Triangle triangle2) {
			return Double.compare(triangle1.area(), triangle2.area());
		}

	}

	public final static class WeightComparator implements Comparator<Triangle> {

		@Override
		public int compare(Triangle triangle1, Triangle triangle2) {
			return Double.compare(triangle1.point2.weight(), triangle2.point2.weight());
		}

	}



}
