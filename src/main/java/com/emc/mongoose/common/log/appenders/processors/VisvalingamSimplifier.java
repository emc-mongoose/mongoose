package com.emc.mongoose.common.log.appenders.processors;

import java.util.LinkedList;
import java.util.List;

public class VisvalingamSimplifier {

	Point[] points;
	List<Triangle> triangles;
	BinaryHeap<Triangle> heap;
	double maxArea;
	int removeLimit;
	int removeCounter;

	public VisvalingamSimplifier(Point[] points) throws IllegalAccessException {
		removeLimit = points.length / 5;
		this.points = points;
		triangles = new LinkedList<>();
		heap = new BinaryHeap<>(13, new Triangle.WeightComparator());
	}

	private Triangle newTriangle(int middlePointIndex) {
		return new Triangle(
				points[middlePointIndex - 1],
				new WeighedPoint(points[middlePointIndex]),
				points[middlePointIndex + 1]);
	}

	public void simplify() {
		if (points.length < 4) {
			return;
		}
		Triangle trianglePrev = null;
		Triangle triangleCurr = newTriangle(1);
		Triangle triangleNext = newTriangle(2);
		triangleCurr.setNext(triangleNext);
		heap.add(triangleCurr);
		for (int i = 3; i < points.length - 1; i++) {
			trianglePrev = triangleCurr;
			triangleCurr = triangleNext;
			triangleNext = newTriangle(i);
			triangleCurr.setPrev(trianglePrev);
			triangleCurr.setNext(triangleNext);
			heap.add(triangleCurr);
		}
		trianglePrev = triangleCurr;
		triangleCurr = triangleNext;
		triangleCurr.setPrev(trianglePrev);
		triangleCurr.setNext(null);
		heap.add(triangleCurr);


		while (removeCounter < removeLimit && !heap.isEmpty()) {
			Triangle triangle = heap.pop();
			if (triangle.weight() < maxArea) {
				triangle.setWeight(maxArea);
			} else {
				maxArea = triangle.weight();
			}
			if (triangle.prev() != null) {
				triangle.prev().setNext(triangle.next());
				triangle.prev().setMiddlePoint(triangle.middlePoint());

			}
		}
	}

	//todo (hint: every time when an element is adding to or removing from a heap, the heap is updating)
	private void update(Triangle triangle) {
	}
}
