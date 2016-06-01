define(['jquery',
		'd3js'],
	function ($,
	          d3) {

		const MARGIN = {
			TOP: 20,
			RIGHT: 20,
			BOTTOM: 30,
			LEFT: 50
		};
		const WIDTH = 960 - MARGIN.LEFT - MARGIN.RIGHT;
		const HEIGHT = 500 - MARGIN.TOP - MARGIN.BOTTOM;

		const parseDate = d3.time.format('%d-%b-%y').parse;

		const defaultsFactory = function () {

			function createDefaultTimeScale() {
				return d3.time.scale();
			}

			function createDefaultLinearScale() {
				return d3.scale.linear();
			}

			function createDefaultAxis() {
				return d3.svg.axis();
			}

			function createDefaultLineGenerator() {
				return d3.svg.line();
			}

			function createDefaultColorizer() {
				return d3.scale.category10();
			}

			return {
				timeScale: createDefaultTimeScale,
				linearScale: createDefaultLinearScale,
				axis: createDefaultAxis,
				lineGenerator: createDefaultLineGenerator,
				colorizer: createDefaultColorizer
			}
		}();

		const X_SCALE = defaultsFactory.timeScale().range([0, WIDTH]);
		const Y_SCALE = defaultsFactory.linearScale().range([HEIGHT, 0]);

		const X_AXIS = defaultsFactory.axis().scale(X_SCALE).orient('bottom').ticks(5);
		const Y_AXIS = defaultsFactory.axis().scale(Y_SCALE).orient('left').ticks(5);

		function xAccessor(data) {
			return data.x;
		}

		function yAccessor(data) {
			return data.y;
		}

		function scaledXAccessor(data) {
			return X_SCALE(xAccessor(data));
		}

		function scaledYAccessor(data) {
			return Y_SCALE(yAccessor(data));
		}

		function extent(array, accessor) {
			return d3.extent(array, accessor);
		}

		function deepExtent(array, accessor) {
			return [
				d3.min(array, function(anArray) {
					return d3.min(anArray, accessor);
				}),
				d3.max(array, function(anArray) {
					return d3.max(anArray, accessor);
				})
			]
		}

		const line = defaultsFactory.lineGenerator().x(scaledXAccessor).y(scaledYAccessor);

		function createSvg(elemSelector) {
			return d3.select(elemSelector)
				.append('svg')
				.attr('width', WIDTH + MARGIN.LEFT + MARGIN.RIGHT)
				.attr('height', HEIGHT + MARGIN.TOP + MARGIN.BOTTOM)
				.append('g')
				.attr('transform', 'translate(' + MARGIN.LEFT + ',' + MARGIN.TOP + ')');
		}

		function handleDataArr(dataArray) {
			dataArray.forEach(function (obj) {
				obj.x = parseDate(obj.x);
				obj.y = +obj.y;
			})
		}

		function drawChart(selector, dataArr) {

			const SVG = createSvg(elemSelector);

			SVG.append('g')
				.attr('class', 'x axis')
				.attr('transform', 'translate(0, ' + HEIGHT + ')')
				.call(X_AXIS);

			SVG.append('g')
				.attr('class', 'y axis')
				.call(Y_AXIS);

			var chart;

			if (Array.isArray(chartArr)) {
				chartArr.forEach(function (chart) {
					handleDataArr(chart)
				});
				X_SCALE.domain(extent(chartArr[0], xAccessor));
				Y_SCALE.domain(deepExtent(chartArr, yAccessor));
				chart = SVG.selectAll('.chart')
					.data(chartArr)
					.enter().append('g')
					.attr('class', 'chart');
				chart.append('path')
					.attr('class', 'line')
					.attr('d', function(chart) {
						return line(chart)
					});
			} else {
				handleDataArr(chartArr);
				X_SCALE.domain(extent(chartArr, xAccessor));
				Y_SCALE.domain(extent(chartArr, yAccessor));
				chart = SVG;
				chart.append('path')
					.attr('class', 'line')
					.attr('d', line(chartArr));
			}
		}

		return {
			drawChart: drawChart
		};
	});