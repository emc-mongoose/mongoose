define(['jquery',
		'd3js',
		'../common/util/templatesUtil',
		'../common/constants'],
	function ($,
	          d3,
	          templatesUtil,
	          constants) {

		const plainId = templatesUtil.composeId;
		const jqId = templatesUtil.composeJqId;

		var currentChartboardSelector;
		var currentChartboardContent;
		var currentChartboardName;
		var currentMetricName;

		const SCALE = {
			LINEAR: 'linear',
			LOG: 'log',
			fullName: function (scaleType, scaleName) {
				return scaleType + ' ' + scaleName + ' scale';
			}
		};

		const SCALE_SWITCH = [
			{
				name: 'x',
				shift: 0
			},
			{
				name: 'y',
				shift: 30
			}
		];

		const MARGIN = {
			TOP: 20,
			RIGHT: 20,
			BOTTOM: 180,
			LEFT: 100
		};
		const WIDTH = 1200 - MARGIN.LEFT - MARGIN.RIGHT;
		const HEIGHT = 600 - MARGIN.TOP - MARGIN.BOTTOM;

		const defaultsFactory = function () {

			function createDefaultTimeScale() {
				return d3.time.scale();
			}

			function createDefaultLinearScale() {
				return d3.scale.linear();
			}

			function createDefaultLogScale() {
				return d3.scale.log();
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
				logScale: createDefaultLogScale,
				axis: createDefaultAxis,
				lineGenerator: createDefaultLineGenerator,
				colorizer: createDefaultColorizer
			}
		}();

		const AXIS_X_WIDTH = Math.round(WIDTH / 1.5);
		const AXIS_Y_WIDTH = HEIGHT;

		function xAccessor(data) {
			return data.x <= 0 ? 0.1 : data.x;
		}

		function yAccessor(data) {
			return data.y <= 0 ? 0.001 : data.y;
			// return data.y;
		}

		var xScale;
		var yScale;
		var xAxis;
		var yAxis;
		var line;

		function setLinearXScale() {
			xScale = defaultsFactory.linearScale().range([0, AXIS_X_WIDTH]);
		}

		function setLinearYScale() {
			yScale = defaultsFactory.linearScale().range([AXIS_Y_WIDTH, 0]);
		}

		function setLogXScale() {
			xScale = defaultsFactory.logScale().range([0, AXIS_X_WIDTH]);
		}

		function setLogYScale() {
			yScale = defaultsFactory.logScale().range([AXIS_Y_WIDTH, 0]);
		}

		function updateAxisX() {
			xAxis = defaultsFactory.axis().scale(xScale).orient('bottom').ticks(5);
		}

		function updateAxisY() {
			yAxis = defaultsFactory.axis().scale(yScale).orient('left').ticks(5);
		}

		function updateLine() {
			line = defaultsFactory.lineGenerator().x(scaledXAccessor).y(scaledYAccessor);
		}

		function switchScaling(scale, axis) {
			switch (scale) {
				case SCALE.LINEAR:
					switch (axis) {
						case 'x':
							setLinearXScale();
							break;
						case 'y':
							setLinearYScale();
							break;
					}
					break;
				case SCALE.LOG:
					switch (axis) {
						case 'x':
							setLogXScale();
							break;
						case 'y':
							setLogYScale();
							break;
					}
					break;
				default:
					setLinearXScale();
					setLinearYScale();
			}
			updateAxisX();
			updateAxisY();
			updateLine();
		}
		
		switchScaling();

		function scaledXAccessor(data) {
			return xScale(xAccessor(data));
		}

		function scaledYAccessor(data) {
			return yScale(yAccessor(data));
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

		const colorizer = defaultsFactory.colorizer();

		function createSvg(parentSelector, svgId) {
			const svgChain = d3.select(parentSelector)
				.append('svg')
				.attr('id', svgId)
				.attr('width', WIDTH + MARGIN.LEFT + MARGIN.RIGHT)
				.attr('height', HEIGHT + MARGIN.TOP + MARGIN.BOTTOM);
			return svgChain.append('g')
				.attr('transform',
					'translate(' + (MARGIN.LEFT + 70) + ',' + (MARGIN.TOP + 70) + ')');
		}

		function createAxes(svgElement) {
			svgElement.append('g')
				.attr('class', 'x-axis axis')
				.attr('transform', 'translate(0, ' + HEIGHT + ')')
				.call(xAxis);

			svgElement.append('text')
				.attr('class', 'x-axis-text axis-text')
				.attr('x', AXIS_X_WIDTH / 2)
				.attr('y', HEIGHT + MARGIN.BOTTOM / 3)
				.style('text-anchor', 'middle')
				.text('t[s]');

			svgElement.append('g')
				.attr('class', 'y-axis axis')
				.call(yAxis);

			svgElement.append('text')
				.attr('class', 'y-axis-text axis-text')
				.attr('transform', 'rotate(-90)')
				.attr('y', 0 - Math.round(MARGIN.LEFT * 1.5))
				.attr('x', 0 - (AXIS_Y_WIDTH / 2))
				.attr('dy', '1em')
				.style('text-anchor', 'middle');

			// svgElement.append("g")
			// 	.attr("class", "x-grid grid")
			// 	.attr("transform", "translate(0," + HEIGHT + ")");
			//
			//
			// svgElement.append("g")
			// 	.attr("class", "y-grid grid");
		}

		function createLabel(svgElement, text) {
			svgElement.append('text')
				.attr('x', (AXIS_X_WIDTH / 2))
				.attr('y', 0 - (MARGIN.TOP / 2))
				.attr('text-anchor', 'middle')
				.style('font-size', '16px')
				.style('text-decoration', 'underline')
				.text(text);
		}

		function createScaleSwitches(svgElement, text) {
			const scaleSwitch = svgElement.selectAll('.scale-switch').data(SCALE_SWITCH);
			const scaleSwitchEnter = scaleSwitch.enter().append('g')
				.attr('class', plainId(['scale', 'switch']))
				.attr('id', function (scaleObj) {
					return plainId(['scale', 'switch', scaleObj.name]);
				})
				.attr('scale', 'linear');
			scaleSwitchEnter.append('circle')
				.attr('cx', 15)
				.attr('cy', function (scaleObj) {
					return HEIGHT + (MARGIN.BOTTOM / 3) + scaleObj.shift;
				})
				.attr('r', 7)
				.style('stroke', 'black')
				.style('stroke-width', 1)
				.style('fill', '#ECE9E9')
				.on('click', function (scaleObj) {
					const switchElem = d3.select(jqId(['scale', 'switch', scaleObj.name]));
					const switchCircle = switchElem.select('circle');
					const switchText = switchElem.select('text');
					if (switchElem.attr('scale') == SCALE.LINEAR) {
						switchCircle.style('fill', 'black');
						switchText.text(function (scaleObj) {
							switchScaling(SCALE.LOG, scaleObj.name);
							return SCALE.fullName(SCALE.LOG, scaleObj.name);
						});
						switchElem.attr('scale', SCALE.LOG);
						updateChartBoardView(currentChartboardSelector, currentMetricName);
					} else {
						switchCircle.style('fill', '#ECE9E9');
						switchText.text(function (scaleObj) {
							switchScaling(SCALE.LINEAR, scaleObj.name);
							return SCALE.fullName(SCALE.LINEAR, scaleObj.name)
						});
						switchElem.attr('scale', SCALE.LINEAR);
						updateChartBoardView(currentChartboardSelector, currentMetricName);
					}
				});
			scaleSwitchEnter.append('text')
				.attr('x', 30)
				.attr('y', function (scaleObj) {
					return HEIGHT + (MARGIN.BOTTOM / 3) + scaleObj.shift;
				})
				.text(function (scaleObj) {
					return SCALE.fullName(SCALE.LINEAR, scaleObj.name)
				});
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

		function createChartBoard(parentSelector, svgId, chartBoardName) {
			const svgCanvasChain = createSvg(parentSelector, svgId);
			createLabel(svgCanvasChain, chartBoardName);
			createAxes(svgCanvasChain);
			createScaleSwitches(svgCanvasChain);
		}

		function updateChartBoardContent(svgSelector, chartBoardName, chartBoardContent, metricName) {
			currentChartboardSelector = svgSelector;
			currentChartboardName = chartBoardName;
			currentChartboardContent = chartBoardContent;
			currentMetricName = metricName;
			updateChartBoardView(svgSelector, metricName);
		}

		function updateAxesLabels(svgElement, metricName) {
			svgElement.select('.y-axis-text')
				.duration(750)
				.text(constants.CHART_METRICS_UNITS_FORMATTER[metricName]);
		}

		function updateAxes(svgElement, chartArr) {
			xScale.domain(extent(chartArr[0], xAccessor));
			yScale.domain(deepExtent(chartArr, yAccessor));
			svgElement.select('.x-axis')
				.call(xAxis);
			svgElement.select('.y-axis')
				.call(yAxis);
			// svgElement.select('.x-grid')
			// 	.call(axisX()
			// 		.tickSize(-HEIGHT, 0, 0)
			// 		.tickFormat(''));
			// svgElement.select('.y-grid')
			// 	.call(axisY()
			// 		.tickSize(-WIDTH, 0, 0)
			// 		.tickFormat(''));
		}

		function updateCharts(svgCanvasElement, chartArr, chartBoardName, metricName) {
			const chart = svgCanvasElement.selectAll('.chart').data(chartArr);
			const chartEnter = chart.enter().append('g')
				.attr('class', 'chart')
				.attr('id', function (chart) {
					return plainId(['id', chartBoardName, metricName, chart.name, 'line']);
				});
			chartEnter.append('path')
				.attr('class', 'line')
				.attr('d', function (chart) {
					return line(chart.values)
				})
				.style('stroke', function (chart) {
					return colorizer(chart.name);
				})
				.style('stroke-width', 1);
			chart.exit().remove();
			const chartUpdate = chart.transition();
			chartUpdate.select('path')
				.duration(750)
				.attr('d', function (chart) {
					return line(chart.values)
				});
		}

		function updateLegend(svgCanvasElement, chartArr, chartBoardName, metricName) {
			const legend = svgCanvasElement.selectAll('.legend').data(chartArr);
			const legendEnter = legend.enter().append('g')
				.attr('class', 'legend')
				.attr('id', function (chart) {
					return plainId(['id', chartBoardName, metricName, chart.name]);
				})
				.on('click', function () {
					const elemented =
						document.getElementById(plainId([this.id, 'line']));
					if ($(this).css('opacity') == 1) {
						d3.select(elemented)
							.transition()
							.duration(1000)
							.style('opacity', 0);
						// .style('display', 'none');
						d3.select(this)
							.transition()
							.duration(1000)
							.style('opacity', .2);
					} else {
						d3.select(elemented)
							.style('display', 'block')
							.transition()
							.duration(1000)
							.style('opacity', 1);
						d3.select(this)
							.transition()
							.duration(1000)
							.style('opacity', 1);
					}
				});
			const legendShift = [0, 30, 60];
			legendEnter.append('circle')
				.attr('cx', AXIS_X_WIDTH + 30)
				.attr('cy', function (chart, index) {
					return legendShift[index];
				})
				.attr('r', 7)
				.style('fill', function (chart) {
					return colorizer(chart.name);
				});
			legendEnter.append('text')
				.attr('x', AXIS_X_WIDTH + 45)
				.attr('y', function (chart, index) {
					return legendShift[index];
				})
				.text(function (chart) {
					return chart.name;
				});
			legend.exit().remove();
			const legendUpdate = legend.transition();
			legendUpdate
				.style('opacity', function () {
					const elemented =
						document.getElementById(plainId([this.id, 'line']));
					d3.select(elemented)
						.style('display', 'block')
						.style('opacity', 1);
					return 1;
				});
			legendUpdate.select('text')
				.text(function (chart) {
					return chart.name;
				});
		}

		function updateChartBoardView(svgSelector, metricName) {
			if (!currentChartboardContent || !currentChartboardName) {
				return;
			}
			currentMetricName = metricName;
			const svg = d3.select(svgSelector).transition();
			updateAxesLabels(svg, metricName);
			const chartArr = currentChartboardContent[metricName];
			const names = [];
			chartArr.forEach(function (chart) {
				handleDataObj(chart);
				names.push(chart.name);
			});
			colorizer.domain(names);
			updateAxes(svg, chartArr);
			const svgCanvas = d3.select(svgSelector + ' g');
			updateCharts(svgCanvas, chartArr, currentChartboardName, metricName);
			updateLegend(svgCanvas, chartArr, currentChartboardName, metricName)
		}

		return {
			createChartBoard: createChartBoard,
			updateChartBoardContent: updateChartBoardContent,
			updateChartBoardView: updateChartBoardView
		};
	});