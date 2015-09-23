/**
 * Created by gusakk on 18.09.15.
 */
define(function() {
	function minHeap(cmp) {
		var heap = {},
			array = [];

		var comparator = compare;

		if (cmp) {
			comparator = cmp;
		}

		heap.push = function(item) {
			up(item.index = array.push(item) - 1);
			return array.length;
		};

		heap.pop = function() {
			var removed = array[0],
				item = array.pop();
			if (array.length) {
				array[item.index = 0] = item;
				down(0);
			}

			return removed;
		};

		heap.remove = function(removed) {
			var i = removed.index,
				item = array.pop();

			if (i != array.length) {
				array[item.index = i] = item;
				((comparator(array[i], removed) < 0) ? up : down)(i);
			}

			return removed;
		};

		function up(i) {
			var parent = (i - 1) >> 1;
			while (i > 0) {
				if (comparator(array[i], array[parent]) >= 0)
					break;
				var childTriangle = array[i],
					parentTriangle = array[parent];
				array[parentTriangle.index = i] = parentTriangle;
				array[childTriangle.index = parent] = childTriangle;

				i = parent;
				parent = (i - 1) >> 1;
			}
		}

		function down(i) {
			var left = 2*i + 1;
			var right = 2*i + 2;
			var min = i;

			if (left < array.length && comparator(array[left], array[min]) < 0) {
				min = left;
			}
			if (right < array.length && comparator(array[right], array[min]) < 0) {
				min = right;
			}
			if (min != i) {
				var currentTriangle = array[i],
					minTriangle = array[min];
				array[minTriangle.index = i] = minTriangle;
				array[currentTriangle.index = min] = currentTriangle;
				down(min);
			}
		}

		function compare(a, b) {
			return a - b;
		}

		heap.print = function() {
			console.log(array);
		};

		return heap;
	}

	return {
		getHeap: minHeap
	};
});