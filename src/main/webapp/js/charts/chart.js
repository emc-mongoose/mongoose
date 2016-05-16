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

		const X_SCALER = d3.time.scale().range([0, WIDTH]);
		const Y_SCALER = d3.scale.linear().range([HEIGHT, 0]);

		const X_AXIS = d3.svg.axis().scale(X_SCALER).orient('bottom').ticks(5);
		const Y_AXIS = d3.svg.axis().scale(Y_SCALER).orient('left').ticks(5);

		function getXData(data) {
			return X_SCALER(data.x);
		}

		function getYData(data) {
			return Y_SCALER(data.y);
		}

		const LINE = d3.svg.line()
			.x(getXData)
			.y(getYData);

		const SVG = d3.select('body')
			.append('svg')
			.attr('width', WIDTH + MARGIN.LEFT + MARGIN.RIGHT)
			.attr('height', HEIGHT + MARGIN.TOP + MARGIN.BOTTOM)
			.append('g')
			.attr('transform', 'translate(' + MARGIN.LEFT + ',' + MARGIN.TOP + ')');

		function drawChart(dataArr) {

			dataArr.forEach(function(obj) {
				obj.x = parseDate(obj.x);
				obj.y = +obj.y;
			});

			X_SCALER.domain(d3.extent(dataArr, function(data) {
				return data.x;
			}));
			Y_SCALER.domain([0, d3.max(dataArr, function(data) {
				return data.y
			})]);

			SVG.append('path')
				.attr('class', 'line')
				.attr('d', LINE(dataArr));

			SVG.append('g')
				.attr('class', 'x axis')
				.attr('transform', 'translate(0, ' + HEIGHT + ')')
				.call(X_AXIS);

			SVG.append('g')
				.attr('class', 'y axis')
				.call(Y_AXIS);
		}

		return {
			drawChart: drawChart
		};
	});