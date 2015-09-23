/**
 * Created by gusakk on 18.09.15.
 */

/*
	Simplifying charts using Visvalingam's algorithm. More info: http://bost.ocks.org/mike/simplify/
 */
define(["./min-heap"], function(minHeap) {

	function simplify(points, pointsToRemove) {

		if (!pointsToRemove) {
			pointsToRemove = points.length / 5;
		}

		var heap = minHeap.getHeap(compare),
			triangles = [],
			triangle = null,
			maxArea = 0,
			deleted = 0;

		function compare(a, b) {
			return a[1][2] - b[1][2];
		}

		var i;
		for (i = 1; i < points.length - 1; i++) {
			triangle = points.slice(i - 1, i + 2);

			if (triangle[1][2] = area(triangle)) {
				triangles.push(triangle);
				heap.push(triangle);
			}
		}

		for (i = 0; i < triangles.length; i++) {
			triangle = triangles[i];
			triangle.next = triangles[i + 1];
			triangle.prev = triangles[i - 1];
		}

		while ((deleted <= pointsToRemove) && (triangle = heap.pop())) {
			if (triangle[1][2] < maxArea) {
				triangle[1][2] = maxArea;
			} else {
				maxArea = triangle[1][2];
			}

			if (triangle.prev) {
				triangle.prev.next = triangle.next;
				triangle.prev[2] = triangle[2];
				update(triangle.prev);
			} else {
				triangle[0][2] = triangle[1][2];
			}

			if (triangle.next) {
				triangle.next.prev = triangle.prev;
				triangle.next[0] = triangle[0];
				update(triangle.next);
			} else {
				triangle[2][2] = triangle[1][2];
			}

			triangle[1][3] = null;

			deleted++;
		}

		function update(triangle) {
			heap.remove(triangle);
			triangle[1][2] = area(triangle);
			heap.push(triangle);
		}

		function area(t) {
			var area;
			try {
				area = Math.abs(
					(t[0].x - t[2].x) * (t[1].y - t[0].y) - (t[0].x - t[1].x) * (t[2].y - t[0].y)
				);
			} catch (err) {
				area = null;
				return false;
			}
			return area;
		}

		return points.filter(function(element) {
			return !element.hasOwnProperty("3");
		});

	}

	return {
		simplify: simplify
	};

});