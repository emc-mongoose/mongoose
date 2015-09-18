/**
 * Created by gusakk on 18.09.15.
 */
require(["./requirejs/conf"], function() {
	require(["./util/visvalingam"], function(vis) {
		var values = [
			{x: 0, y: 0},
			{x: 1, y: 1},
			{x: 2, y: 8},
			{x: 3, y: 3},
			{x: 4, y: 4},
			{x: 5, y: 5}
		];

		var arr = [
			[0, 0],
			[1, 1],
			[2, 8],
			[3, 3],
			[4, 4],
			[5, 5]
		];

		vis.simplify(values);
	});
});



