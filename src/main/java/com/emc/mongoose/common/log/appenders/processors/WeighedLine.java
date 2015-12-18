package com.emc.mongoose.common.log.appenders.processors;

import java.util.ArrayList;
import java.util.List;

class WeighedLine {

	Point firstPoint, lastPoint;
	List<WeighedPoint> points;

	public WeighedLine(Point... points) {
		firstPoint = points[0];
		lastPoint = points[points.length - 1];
		this.points = new ArrayList<>();
		if (points.length > 2) {
			for (int i = 1; i < points.length - 1; i++) {
				this.points.add(new WeighedPoint(points[i],
						triangleArea(
								points[i - 1],
								points[i],
								points[i + 1])));
			}
		}
	}

	private double triangleArea(Point point1, Point point2, Point point3) {
		double a = point1.distance(point2);
		double b = point2.distance(point3);
		double c = point3.distance(point1);
		double p = (a + b + c) / 2;
		return Math.sqrt(p * (p - a) * (p - b) * (p - c));
	}

	private void setWeightForPoint(int pointIndex) {
		int size = points.size();
		if (size == 1) {
			setWeightForOnlyOnePoint();
		} else if (size == 2) {
			setWeightForSecondPoint();
			setWeightForPenultPoint();
		} else {
			if (pointIndex == 0) {
				setWeightForSecondPoint();
			} else if (pointIndex == points.size()) {
				setWeightForPenultPoint();
			} else {
				setWeightForInternalPoint(pointIndex);
				setWeightForInternalPoint(pointIndex + 1);
			}
		}
	}

	private void setWeightForInternalPoint(int pointIndex) {
		points.get(pointIndex).setWeight(
				computePointWeight(pointIndex));
	}

	private double computePointWeight(int pointIndex) {
		return triangleArea(
				points.get(pointIndex - 1).point(),
				points.get(pointIndex).point(),
				points.get(pointIndex + 1).point());
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

	// todo queue needs to be updated
	public void removeAndWeigh(WeighedPoint point) {
		int index = points.indexOf(point);
		points.remove(index);
		if (points.size() != 0) {
			setWeightForPoint(index);
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(firstPoint).append("\n");
		for (WeighedPoint point : points) {
			builder.append(point).append("\n");
		}
		builder.append(lastPoint).append("\n");
		return builder.toString();
	}


}
