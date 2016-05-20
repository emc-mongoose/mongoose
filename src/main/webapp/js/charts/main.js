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

		const scaleX = defaultsFactory.linearScale().range([0, WIDTH]);
		const scaleY = defaultsFactory.linearScale().range([HEIGHT, 0]);

		const axisX = defaultsFactory.axis().scale(scaleX).orient('bottom').ticks(5);
		const axisY = defaultsFactory.axis().scale(scaleY).orient('left').ticks(5);

		function xAccessor(data) {
			return data.x;
		}

		function yAccessor(data) {
			return data.y;
		}

		function scaledXAccessor(data) {
			return scaleX(xAccessor(data));
		}

		function scaledYAccessor(data) {
			return scaleY(yAccessor(data));
		}

		function extent(array, accessor) {
			return d3.extent(array.values, accessor);
		}

		function deepExtent(array, accessor) {
			return [
				d3.min(array, function (anArray) {
					return d3.min(anArray.values, accessor);
				}),
				d3.max(array, function (anArray) {
					return d3.max(anArray.values, accessor);
				})
			]
		}

		const line = defaultsFactory.lineGenerator().x(scaledXAccessor).y(scaledYAccessor);
		const colorizer = defaultsFactory.colorizer();

		function createSvg(parentSelector) {
			d3.select(parentSelector)
				.append('svg')
			.attr('width', WIDTH + MARGIN.LEFT + MARGIN.RIGHT)
			.attr('height', HEIGHT + MARGIN.TOP + MARGIN.BOTTOM)
				.append('g')
				.attr('transform', 'translate(' + MARGIN.LEFT + ',' + MARGIN.TOP + ')');
		}

		function appendAxes(svgElement) {
			svgElement.append('g')
				.attr('class', 'x axis')
				.attr('transform', 'translate(0, ' + HEIGHT + ')')
				.call(axisX);

			svgElement.append('g')
				.attr('class', 'y axis')
				.call(axisY);
		}

		function handleDataObj(dataObj) {
			var values = dataObj.values;
			if (values.length > 0) {
				if (values[values.length - 1] === null) {
					values.pop();
				}
				values.forEach(function (point) {
					if (point !== null) {
						point.x = +point.x;
						point.y = +point.y;
					}
				})
			}
		}

		function drawChart(svgSelector, chartObj) {

			const SVG = d3.select(svgSelector);

			SVG.selectAll('*').remove();

			var chart;

			if (Array.isArray(chartObj)) {
				const names = [];
				chartObj.forEach(function (chart) {
					handleDataObj(chart);
					names.push(chart.name);
				});
				colorizer.domain(names);
				scaleX.domain(extent(chartObj[0], xAccessor));
				scaleY.domain(deepExtent(chartObj, yAccessor));
				appendAxes(SVG);
				chart = SVG.selectAll('.chart')
					.data(chartObj)
					.enter().append('g')
					.attr('class', 'chart');
				chart.append('path')
					.attr('class', 'line')
					.attr('d', function (chart) {
						return line(chart.values)
					})
					.style('stroke', function (chart) {
						return colorizer(chart.name);
					})
			} else {
				handleDataObj(chartObj);
				scaleX.domain(extent(chartObj, xAccessor));
				scaleY.domain(extent(chartObj, yAccessor));
				appendAxes(SVG);
				chart = SVG;
				chart.append('path')
					.attr('class', 'line')
					.attr('d', line(chartObj.values));
			}
		}

		return {
			drawChart: drawChart,
			createSvg: createSvg
		};
	});