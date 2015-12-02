define([
	"d3js",
	"../util/common",
	"../util/visvalingam",
	"../../util/constants"
], function(
	d3, common, vis, constants
) {
	var margin = common.getMargin(),
		width = common.getWidth(),
		height = common.getHeight();

	function initDataArray(to, from, chartType, metricsPeriodSec) {
		return simplifyChart(to, from, chartType, metricsPeriodSec);
	}

	function simplifyChart(to, from, chartType, metricsPeriodSec) {
		var currMetricsPeriodSec = -metricsPeriodSec;
		from.forEach(function(d) {
			var value = common.parsePerfAvgLogEvent(chartType, d.message.formattedMessage);
			currMetricsPeriodSec += metricsPeriodSec;
			to.forEach(function(item, i) {
				if (item.values.length === constants.getPointsOnChartLimit()) {
					item.values = vis.simplify(item.values);
				}
				item.values.push({x: currMetricsPeriodSec, y: parseFloat(value[i])});
			});
		});
		return currMetricsPeriodSec;
	}


	function clearArrays(array) {
		array.forEach(function(d) {
			return d.values.shift();
		});
	}

	function drawThroughputCharts(data, json, sec) {
		var updateFunction = drawChart(data, json, "t[seconds]", "Rate[op/s]",
			"#tp-" + json.contextMap[constants.getCfgConstants().runId].split(".").join("_"),
			sec);
		return {
			update: function(json) {
				updateFunction(constants.getChartTypes().TP, json.message.formattedMessage);
			}
		};
	}

	function drawBandwidthCharts(data, json, sec) {
		var updateFunction = drawChart(data, json, "t[seconds]", "Rate[MB/s]",
			"#bw-" + json.contextMap[constants.getCfgConstants().runId].split(".").join("_"),
			sec);
		return {
			update: function(json) {
				updateFunction(constants.getChartTypes().BW, json.message.formattedMessage);
			}
		};
	}

	function drawLatencyCharts(data, json, sec) {
		var updateFunction = drawChart(data, json, "t[seconds]", "Latency[mcSec]",
			"#lat-" + json.contextMap[constants.getCfgConstants().runId].split(".").join("_"),
				sec);
		return {
			update: function(json) {
				updateFunction(constants.getChartTypes().LAT, json.message.formattedMessage);
			}
		};
	}

	function drawDurationCharts(data, json, sec) {
		var updateFunction = drawChart(data, json, "t[seconds]", "Duration[mcSec]",
			"#dur-" + json.contextMap[constants.getCfgConstants().runId].split(".").join("_"),
				sec);
		return {
			update: function(json) {
				updateFunction(constants.getChartTypes().DUR, json.message.formattedMessage);
			}
		};
	}

	function drawChart(data, json, xAxisLabel, yAxisLabel, chartDOMPath, sec) {
		var scaleTypes = constants.getScaleTypes();
		var scalesOrientation = constants.getScaleOrientations();
		//
		var currXScale = scaleTypes[0];
		var currYScale = scaleTypes[0];
		//  get some fields from runTimeConfig
		var runMetricsPeriodSec = json.contextMap[constants.getCfgConstants().runMetricsPeriodSec];
		//
		json.threadName = json.threadName.match(common.getThreadNamePattern())[0];
		//
		var currentMetricsPeriodSec = 0;
		if (sec !== undefined) {
			currentMetricsPeriodSec = sec;
		}
		//
		var currTimeUnit = constants.getTimeLimit().seconds;
		//
		var x = d3.scale.linear()
			.domain([
				d3.min(data, function(c) { return d3.min(c.values, function(d) { return d.x; }); }),
				d3.max(data, function(c) { return d3.max(c.values, function(d) { return d.x; }); })
			])
			.range([0, width]);
		//
		while (common.isTimeLimitReached(x.domain()[x.domain().length - 1], currTimeUnit)) {
			if (currTimeUnit.next === null) {
				return;
			}
			currTimeUnit = constants.getTimeLimit()[currTimeUnit.next];
			xAxisLabel = currTimeUnit.label;
			x = d3.scale.linear()
				.domain([
					d3.min(data, function(c) { return d3.min(c.values, function(d) { return d.x / currTimeUnit.value; }); }),
					d3.max(data, function(c) { return d3.max(c.values, function(d) { return d.x / currTimeUnit.value; })  })
				])
				.range([0, width]);
		}
		//
		x = d3.scale.linear()
			.domain([
				d3.min(data, function(c) { return d3.min(c.values, function(d) { return d.x / currTimeUnit.value; }); }),
				d3.max(data, function(c) { return d3.max(c.values, function(d) { return d.x / currTimeUnit.value; })  })
			])
			.range([0, width]);
		//
		var y = d3.scale.linear()
			.domain([
				d3.min(data, function(c) { return d3.min(c.values, function(d) { return d.y; }); }),
				d3.max(data, function(c) { return d3.max(c.values, function(d) { return d.y; })  })
			])
			.nice()
			.range([height, 0]);
		//
		var color = d3.scale.ordinal()
			.domain(data.map(function(d) { return d.name.id; }))
			.range(constants.getChartColors());
		//
		var xAxis = d3.svg.axis()
			.scale(x)
			.orient("bottom");
		var yAxis = d3.svg.axis()
			.scale(y)
			.orient("left");

		function makeXAxis() {
			return d3.svg.axis()
				.scale(x)
				.orient("bottom");
		}

		function makeYAxis() {
			return d3.svg.axis()
				.scale(y)
				.orient("left");
		}
		//
		var line = d3.svg.line()
			.x(function (d) {
				return x((isNaN(x(d.x))) ? (0.1 / currTimeUnit.value)
					: (d.x / currTimeUnit.value));
			})
			.y(function (d) {
				return y((isNaN(y(d.y))) ? 0.001 : d.y);
			});
		//
		var svg = d3.select(chartDOMPath)
			.append("div")
			.attr("class", "svg-container")
			.append("svg")
			.attr("width", width + margin.left + margin.right)
			.attr("height", height + margin.top + margin.bottom)
			.append("g")
			.attr("transform", "translate(" + margin.left + "," + margin.top + ")")
			.style("font", "12px sans-serif")
			.style("overflow-x", "auto");
		//
		//  Axis X Label
		var horizontalLabel = svg.append("text")
			.attr("x", (width + margin.left)/2)
			.attr("y", height + margin.top)
			.style("text-anchor", "end")
			.text(xAxisLabel);
		//
		var xAxisGroup = svg.append("g")
			.attr("class", "x axis")
			.attr("transform", "translate(0," + height + ")")
			.call(xAxis);

		var yAxisGroup = svg.append("g")
			.attr("class", "y axis")
			.call(yAxis);

		var xGrid = svg.append("g")
			.attr("class", "grid")
			.attr("transform", "translate(0," + height + ")")
			.call(makeXAxis()
				.tickSize(-height, 0, 0)
				.tickFormat(""));

		var yGrid = svg.append("g")
			.attr("class", "grid")
			.call(makeYAxis()
				.tickSize(-width, 0, 0)
				.tickFormat(""));
		//
		var levels = svg.selectAll(".level")
			.data(data).enter()
			.append("g")
			.attr("class", "level")
			.attr("id", function(d, i) { return chartDOMPath.replace("#", "") + d.name.id; })
			.attr("visibility", function(d) { if (d.name.id === constants.getAvgConstant().id) {
				return "visible"; } else { return "hidden"; }
			})
			.append("path")
			.attr("class", "line")
			.attr("d", function(d)  { return line(d.values); })
			.attr("stroke", function(d) { return color(d.name.id); })
			.attr("fill", "none");

		//  Axis Y Label
		svg.append("text")
			.attr("transform", "rotate(-90)")
			.attr("y", -50)
			.attr("x", -(height/2) + 30)
			.attr("dy", ".71em")
			.style("text-anchor", "end")
			.text(yAxisLabel);
		//
		svg.selectAll("foreignObject")
			.data(data).enter()
			.append("foreignObject")
			.attr("class", "foreign")
			.attr("x", width + 3)
			.attr("width", 18)
			.attr("height", 18)
			.attr("transform", function(d, i) {
				return "translate(0," + i * 20 + ")";
			})
			.append("xhtml:body")
			.append("input")
			.attr("type", "checkbox")
			.attr("value", function(d) { return d.name.id; })
			.attr("checked", function(d) { if (d.name.id === constants.getAvgConstant().id) {
				return "checked"; }
			})
			.on("click", function(d, i) {
				var element = $(chartDOMPath + d.name.id);
				if ($(this).is(":checked")) {
					element.css("visibility", "visible")
				} else {
					element.css("visibility", "hidden");
				}
			});
		function redraw(currXScale, currYScale) {
			switch (currXScale) {
				case scaleTypes[0]:
					xAxisGroup.transition().call(d3.svg.axis().scale(x).orient("bottom"));
					xGrid.call(d3.svg.axis().scale(x)
						.tickSize(-height, 0, 0)
						.tickFormat(""));
					xGrid.selectAll(".minor")
						.style("stroke-opacity", "0.7")
						.style("shape-rendering", "crispEdges")
						.style("stroke", "lightgrey")
						.style("stroke-dasharray", "0");
					break;
				case scaleTypes[1]:
					xAxisGroup.transition().call(d3.svg.axis().scale(x).orient("bottom").ticks(0, ".1s"));
					xGrid.call(d3.svg.axis().scale(x)
						.tickSize(-height, 0, 0)
						.tickFormat(""))
						.selectAll(".tick")
						.data(x.ticks().map(x.tickFormat(0, ".1")), function(d) { return d; })
						.exit()
						.classed("minor", true);
					xGrid.selectAll(".minor")
						.style("stroke-opacity", "0.5")
						.style("shape-rendering", "crispEdges")
						.style("stroke-dasharray", "2,2");
					break;
			}
			switch (currYScale) {
				case scaleTypes[0]:
					yAxisGroup.transition().call(d3.svg.axis().scale(y).orient("left"));
					yGrid.call(d3.svg.axis().scale(y).orient("left")
						.tickSize(-width, 0, 0)
						.tickFormat(""));
					yGrid.selectAll(".minor")
						.style("stroke-opacity", "0.7")
						.style("shape-rendering", "crispEdges")
						.style("stroke", "lightgrey")
						.style("stroke-dasharray", "0");
					break;
				case scaleTypes[1]:
					yAxisGroup.transition().call(d3.svg.axis().scale(y).orient("left").ticks(0, ".1s"));
					yGrid.call(d3.svg.axis().scale(y).orient("left")
						.tickSize(-width, 0, 0)
						.tickFormat(""))
						.selectAll(".tick")
						.data(y.ticks().map(y.tickFormat(0, ".1")), function(d) { return d; })
						.exit()
						.classed("minor", true);
					yGrid.selectAll(".minor")
						.style("stroke-opacity", "0.5")
						.style("shape-rendering", "crispEdges")
						.style("stroke-dasharray", "2,2");
					break;
			}
			//  Update old charts
			var paths = svg.selectAll(".level path")
				.data(data)
				.attr("d", function(d) { return line(d.values); })
				.attr("stroke", function(d) { return color(d.name.id); })
				.attr("fill", "none");

			//
			d3.selectAll(".axis path, .axis line")
				.style("fill", "none")
				.style("stroke", "grey")
				.style("stroke-width", "1")
				.style("shape-rendering", "crispEdges");
			//
			d3.selectAll(".grid .tick")
				.style("stroke", "lightgrey")
				.style("stroke-opacity", "0.7")
				.style("shape-rendering", "crispEdges");
			//
		}
		//  checkboxes for linear/log scales
		common.appendScaleLabels(svg, {
			updateScales: function(scaleOrientation, scaleType) {
				// find scale and apply params to it
				if (scaleType === scaleTypes[0]) {
					if (scaleOrientation === scalesOrientation[0]) {
						x = d3.scale.linear()
							.domain([d3.min(data, function(c) { return d3.min(c.values, function(d) {
								return d.x / currTimeUnit.value;
							}); }),
								d3.max(data, function(c) { return d3.max(c.values, function(d) {
									return d.x / currTimeUnit.value;
								}); })])
							.range([0, width]);
						currXScale = scaleTypes[0];
					} else {
						y = d3.scale.linear()
							.domain([d3.min(data, function(c) { return d3.min(c.values, function(d) {
								return d.y;
							}); }),
								d3.max(data, function(c) { return d3.max(c.values, function(d) {
									return d.y;
								}); })])
							.nice()
							.range([height, 0]);
						currYScale = scaleTypes[0];
					}
				} else {
					if (scaleOrientation === scalesOrientation[0]) {
						x = d3.scale.log()
							.domain([d3.min(data, function(c) { return d3.min(c.values, function(d) {
								return d.x <= 0 ? (0.1 / currTimeUnit.value)
									: (d.x / currTimeUnit.value);
							}); }),
								d3.max(data, function(c) { return d3.max(c.values, function(d) {
									return d.x <= 0 ? (0.1 / currTimeUnit.value)
										: (d.x / currTimeUnit.value);
								}); })])
							.range([0, width]);
						currXScale = scaleTypes[1];
					} else {
						y = d3.scale.log()
							.domain([d3.min(data, function(c) { return d3.min(c.values, function(d) {
								return d.y <= 0 ? 0.001 : d.y;
							}); }),
								d3.max(data, function(c) { return d3.max(c.values, function(d) {
									return d.y <= 0 ? 0.001 : d.y;
								}); })])
							.nice()
							.range([height, 0]);
						currYScale = scaleTypes[1];
					}
				}
				redraw(currXScale, currYScale);
			}
		}, 0);
		//
		var legend = svg.selectAll(".legend")
			.data(data).enter()
			.append("g")
			.attr("class", "legend")
			.attr("transform", function(d, i) {
				return "translate(0," + i * 20 + ")";
			});

		legend.append("rect")
			.attr("x", width + 18)
			.attr("width", 18)
			.attr("height", 18)
			.style("fill", function(d) { return color(d.name.id); });

		legend.append("text")
			.attr("x", width + 40)
			.attr("y", 9)
			.attr("dy", ".35em")
			.style("text-anchor", "start")
			.text(function(d) { return d.name.text; });

		svg.append("text")
			.attr("x", (width / 2))
			.attr("y", 0 - (margin.top / 2))
			.attr("text-anchor", "middle")
			.style("font-size", "16px")
			.style("text-decoration", "underline")
			.text(json.threadName);
		//
		d3.selectAll(".axis path, .axis line")
			.style("fill", "none")
			.style("stroke", "grey")
			.style("stroke-width", "1")
			.style("shape-rendering", "crispEdges");
		//
		d3.selectAll(".grid .tick")
			.style("stroke", "lightgrey")
			.style("stroke-opacity", "1")
			.style("shape-rendering", "crispEdges");
		//
		d3.selectAll(".minor")
			.style("stroke-opacity", "0.5")
			.style("shape-rendering", "crispEdges")
			.style("stroke-dasharray", "2,2");
		//
		d3.select(chartDOMPath)
			.append("a")
			.text("Save chart")
			.style("cursor", "pointer")
			.style("margin-left", margin.left + "px")
			.on("click", function() {
				common.saveChart(chartDOMPath, 1070, 460);
			});
		return function(chartType, value) {
			currentMetricsPeriodSec += parseInt(runMetricsPeriodSec);
			//
			var parsedValue = common.parsePerfAvgLogEvent(chartType, value);
			//
			data.forEach(function(d, i) {
				if (d.values.length === constants.getPointsOnChartLimit()) {
					d.values = vis.simplify(d.values);
				}
				d.values.push({x: currentMetricsPeriodSec, y: parseFloat(parsedValue[i])});
			});

			//
			while (common.isTimeLimitReached(x.domain()[x.domain().length - 1], currTimeUnit)) {
				if (currTimeUnit.next === null) {
					return;
				}
				currTimeUnit = constants.getTimeLimit()[currTimeUnit.next];
				horizontalLabel.text(currTimeUnit.label);
				x.domain([
					d3.min(data, function(c) { return d3.min(c.values, function(d) {
						return (isNaN(x(d.x))) ? (0.1 / currTimeUnit.value)
							: (d.x / currTimeUnit.value);
					}); }),
					d3.max(data, function(c) { return d3.max(c.values, function(d) {
						return (isNaN(x(d.x))) ? (0.1 / currTimeUnit.value)
							: (d.x / currTimeUnit.value);
					}); })
				]);
			}
			//
			x.domain([
				d3.min(data, function(c) { return d3.min(c.values, function(d) {
					return (isNaN(x(d.x))) ? (0.1 / currTimeUnit.value)
						: (d.x / currTimeUnit.value);
				}); }),
				d3.max(data, function(c) { return d3.max(c.values, function(d) {
					return (isNaN(x(d.x))) ? (0.1 / currTimeUnit.value)
						: (d.x / currTimeUnit.value);
				}); })
			]);
			y.domain([
				d3.min(data, function(c) { return d3.min(c.values, function(d) {
					return (isNaN(y(d.y))) ? 0.001 : d.y }); }),
				d3.max(data, function(c) { return d3.max(c.values, function(d) {
					return (isNaN(y(d.y))) ? 0.001 : d.y
				}); })
			])
				.nice();
			//
			redraw(currXScale, currYScale);
		};
	}

	return {
		initDataArray: initDataArray,
		simplifyChart: simplifyChart,
		clearArrays: clearArrays,
		drawThroughputCharts: drawThroughputCharts,
		drawBandwidthCharts: drawBandwidthCharts,
		drawLatencyCharts: drawLatencyCharts,
		drawDurationCharts: drawDurationCharts
	};
});