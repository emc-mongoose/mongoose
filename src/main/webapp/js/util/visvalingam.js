/**
 * Created by gusakk on 18.09.15.
 */
define(["./min-heap"], function(minHeap) {

	var simplify = function(coordinates) {

		var heap = minHeap.getHeap(),
			maxArea = 0,
			triangles = [],
			triangle;

		var i;

		for (i = 1; i < coordinates.length; i++) {
			triangle = coordinates.slice(i - 1, i + 2);
			if (triangle[1].area = area(triangle)) {
				triangles.push(triangle);
				heap.push(triangle);
			}
		}

		for (i = 0; i < triangles.length; i++) {
			triangles[i].next = triangles[i + 1];
			triangles[i].prev = triangles[i - 1];
		}

		while (triangle = heap.pop()) {
			if (triangle[1].area < maxArea) {
				triangle[1].area = maxArea;
			} else {
				maxArea = triangle[1].area;
			}

			if (triangle.prev) {
				triangle.prev.next = triangle.next;
				triangle.prev[2] = triangle[2];
				update(triangle.prev);
			} else {
				triangle[0].area = triangle[1].area;
			}

			if (triangle.next) {
				triangle.next.prev = triangle.prev;
				triangle.next[0] = triangle[0];
				update(triangle.next);
			} else {
				triangle[2].area = triangle[1].area;
			}
		}

		function update(triangle) {
			heap.remove(triangle);
			triangle[1].area = area(triangle);
			heap.push(triangle);
		}

		console.log(coordinates);
	};

	function area(t) {
		if (t.length != 3)
			return;
		return Math.abs(
			(t[0].x - t[2].x) * (t[1].y - t[0].y) - (t[0].x - t[1].x) * (t[2].y - t[0].y)
		);
	}

	return {
		simplify: simplify
	};

});