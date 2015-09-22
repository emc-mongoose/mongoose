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
			up(array.push(item) - 1);
			return array.length;
		};

		heap.pop = function() {
			var removed = array[0];
			array[0] = array[array.length - 1];
			array.pop();
			down(0);

			return removed;
		};

		heap.remove = function(index) {
			var removed;
			for (var i = 0; i < array.length; i++) {
				if (array[i].index == index) {
					removed = array[i];
					break;
				}
			}
			if (removed) {
				array[i] = array.pop();
				((comparator(array[i], removed) < 0) ? up : down)(i);
			}

			return removed.index;
		};

		function up(i) {
			var parent = (i - 1) >> 1;
			var temp;
			while (i > 0) {
				if (comparator(array[i], array[parent]) >= 0)
					break;
				temp = array[i];
				array[i] = array[parent];
				array[parent] = temp;

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
				var temp = array[min];
				array[min] = array[i];
				array[i] = temp;
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