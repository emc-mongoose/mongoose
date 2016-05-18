package com.emc.mongoose.core.impl.load.tasks.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static com.emc.mongoose.core.impl.load.tasks.processors.Point.distance;
import static java.lang.Math.sqrt;

class Polyline {

	private Point firstPoint, lastPoint;
	private List<WeighedPoint> points = new ArrayList<>();
	private PriorityQueue<WeighedPoint> queue = new PriorityQueue<>();

	private double triangleArea(final Point point1, final Point point2, final Point point3) {
		double a = distance(point1, point2);
		double b = distance(point2, point3);
		double c = distance(point3, point1);
		double p = (a + b + c) / 2;
		return sqrt(p * (p - a) * (p - b) * (p - c));
	}

	private double triangleArea(final List<Point> trianglePoints) {
		return triangleArea(trianglePoints.get(0), trianglePoints.get(1), trianglePoints.get(2));
	}

	private List<Point> getTriangle(final int middlePointIndex) {
		List<Point> result = new ArrayList<>();
		for (WeighedPoint weighedPoint: points.subList(middlePointIndex - 1, middlePointIndex + 2)) {
			result.add(weighedPoint.point());
		}
		return result;
	}

	private void setWeightForPoint(final int pointIndex) {
		int size = points.size();
		if (size == 1) {
			setWeightForOnlyOnePoint();
		} else if (size == 2) {
			setWeightForSecondPoint();
			setWeightForPenultPoint();
		} else {
			if (pointIndex == 0 || pointIndex == 1) {
				setWeightForSecondPoint();
			} else if (pointIndex == size - 1 || pointIndex == size) {
				setWeightForPenultPoint();
			}  else {
				setWeightForInternalPoints(pointIndex);
			}
		}
	}

	private void setWeightForInternalPoints(final int pointIndex) {
		points.get(pointIndex).setWeight(
				triangleArea(getTriangle(pointIndex)));
		points.get(pointIndex + 1).setWeight(
				triangleArea(getTriangle(pointIndex + 1)));
	}

	private void setWeightForOnlyOnePoint() {
		points.get(0).setWeight(triangleArea(
				firstPoint,
				points.get(0).point(),
				lastPoint));
	}

	private void setWeightForSecondPoint() {
		WeighedPoint secondPoint = points.get(0);
		secondPoint.setWeight(triangleArea(
				firstPoint,
				secondPoint.point(),
				points.get(1).point()));
	}

	private void setWeightForPenultPoint() {
		WeighedPoint penultPoint = points.get(points.size() - 1);
		penultPoint.setWeight(triangleArea(
				points.get(points.size() - 2).point(),
				penultPoint.point(),
				lastPoint));
	}

	private void removeAndWeigh(final WeighedPoint point) {
		int index = points.indexOf(point);
		points.remove(index);
		if (points.size() != 0) {
			setWeightForPoint(index);
		}
	}

	public void addPoint(final Point newPoint) {
		switch (numberOfPoints()) {
			case 0:
				firstPoint = newPoint;
				break;
			case 1:
				lastPoint = newPoint;
				break;
			case 2:
				addNewLastPoint(newPoint, firstPoint);
				break;
			default:
				addNewLastPoint(newPoint, points.get(points.size() - 1).point());
		}
	}

	private void addNewLastPoint(final Point newPoint, final Point penultPoint) {
		WeighedPoint oldLastPoint = new WeighedPoint(lastPoint,
				triangleArea(
						penultPoint,
						lastPoint,
						newPoint
				));
		points.add(oldLastPoint);
		queue.add(oldLastPoint);
		lastPoint = newPoint;
	}

	public void simplify(final int simplificationsNum) {
		if (simplificationsNum >= 0 && simplificationsNum <= points.size()) {
			for (int i = 0; i < simplificationsNum; i++) {
				this.removeAndWeigh(queue.peek());
				queue.poll();
//				System.out.printf("Simplification %d:\n%s\n", i + 1, this);
			}
		}
	}

	public int numberOfPoints() {
		if (lastPoint != null) {
			return points.size() + 2;
		} else if (firstPoint != null) {
			return 1;
		} else {
			return 0;
		}
	}

	public List<Point> getPoints() {
		List<Point> points = new ArrayList<>();
		points.add(firstPoint);
		for (WeighedPoint weighedPoint: this.points) {
			points.add(weighedPoint.point());
		}
		points.add(lastPoint);
		return points;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		switch (numberOfPoints()) {
			case 0:
				builder.append("empty").append("\n");
				break;
			case 1:
				builder.append(firstPoint).append("\n");
				break;
			case 2:
				builder.append(firstPoint).append("\n");
				builder.append(lastPoint).append("\n");
				break;
			default:
				builder.append(firstPoint).append("\n");
				for (WeighedPoint point : points) {
					builder.append(point).append("\n");
				}
				builder.append(lastPoint).append("\n");
		}
		return builder.toString();
	}



}
