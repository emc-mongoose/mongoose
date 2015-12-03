define([
	"d3js",
	"../util/common",
	"../util/visvalingam",
	"../../util/constants"
], function(
	d3,
    common,
    vis,
    constants
) {
	var margin = common.getMargin(),
		width = common.getWidth(),
		height = common.getHeight();

	function initDataArray(destArray, dataArray, chartType, runMetricsPeriodSec) {
		//
		var loadTypes = [];
		dataArray.filter(function(d) {
			var loadType = d.threadName.match(common.getThreadNamePattern())[0];
			var result = this.indexOf(loadType) === -1;
			if (result === true) {
				this.push(loadType);
			}
			return result;
		}, loadTypes);
		destArray[0].loadType = loadTypes[0];
		var i = 1;
		while (i < loadTypes.length) {
			var firstElement = $.extend(true, {}, destArray[0]);
			firstElement.loadType = loadTypes[i];
			destArray.push(firstElement);
			i++;
		}
		//
		dataArray.forEach(function(d) {
			var value = common.parsePerfAvgLogEvent(chartType, d.message.formattedMessage);
			var loadType = d.threadName.match(common.getThreadNamePattern())[0];
			destArray.forEach(function(d) {
				if (d.loadType === loadType) {
					isFound = true;
					d.currentRunMetricsPeriodSec += parseInt(runMetricsPeriodSec);
					d.charts.forEach(function(c, i) {
						if (c.values.length === constants.getPointsOnChartLimit()) {
							c.values = vis.simplify(c.values);
						}
						c.values.push({x: d.currentRunMetricsPeriodSec, y: parseFloat(value[i])});
					})
				}
			});
		});
	}
	//
	function clearArrays(array, metricsPeriodSec) {
		array.forEach(function(d) {
			d.charts.forEach(function(c) {
				c.values.shift();
			});
			d.currentRunMetricsPeriodSec = -parseInt(metricsPeriodSec);
		});
	}

	//
	function drawThroughputChart(data, runId, metricsSec) {
		var updateFunction = drawChart(
			data, "Throughput[obj/s]", "t[seconds]", "Rate[op/s]", "#tp-" + runId.split(".").join("_"), metricsSec, false
		);
		return {
			update: function(json) {
				updateFunction(constants.getChartTypes().TP, json);
			}
		};
	}
	//
	function drawBandwidthChart(data, runId, metricsSec) {
		var updateFunction = drawChart(
			data, "Bandwidth[MB/s]", "t[seconds]", "Rate[MB/s]", "#bw-" + runId.split(".").join("_"), metricsSec, false
		);
		return {
			update: function(json) {
				updateFunction(constants.getChartTypes().BW, json);
			}
		};
	}

	function drawLatencyChart(data, runId, metricsSec) {
		var updateFunction = drawChart(
			data, "Latency[us]", "t[seconds]", "Latency[us]", "#lat-" + runId.split(".").join("_"), metricsSec, true
		);
		return {
			update: function(json) {
				updateFunction(constants.getChartTypes().LAT, json);
			}
		};
	}

	function drawDurationChart(data, runId, metricsSec) {
		var updateFunction = drawChart(
			data, "Duration[us]", "t[seconds]", "Duration[us]", "#dur-" + runId.split(".").join("_"), metricsSec, true
		);
		return {
			update: function(json) {
				updateFunction(constants.getChartTypes().DUR, json);
			}
		};
	}

	function drawChart(data, chartTitle, xAxisLabel, yAxisLabel, path, metricsSec, isLatencyChart) {
		//
		var LAT_MODES = [constants.getAvgConstant(), constants.getMinConstant(), constants.getMaxConstant()];
		var TP_MODES = [constants.getAvgConstant(), constants.getLastConstant()];
		var scaleTypes = constants.getScaleTypes();
		var scalesOrientation = constants.getScaleOrientations();
		var timeLimit = constants.getTimeLimit();

		var currXScale = scaleTypes[0];
		var currYScale = scaleTypes[0];
		//
		var currTimeUnit = timeLimit.seconds;
		//
		var x = d3.scale.linear()
			.domain([
				d3.min(data, function(d) { return d3.min(d.charts, function(c) {
					return d3.min(c.values, function(v) { return v.x; }); });
				}),
				d3.max(data, function(d) { return d3.max(d.charts, function(c) {
					return d3.max(c.values, function(v) { return v.x; }); });
				})
			])
			.range([0, width]);

		while (common.isTimeLimitReached(x.domain()[x.domain().length - 1], currTimeUnit)) {
			if (currTimeUnit.next === null) {
				return;
			}
			currTimeUnit = timeLimit[currTimeUnit.next];
			xAxisLabel = currTimeUnit.label;
			x = d3.scale.linear()
				.domain([
					d3.min(data, function(d) { return d3.min(d.charts, function(c) {
						return d3.min(c.values, function(v) { return v.x / currTimeUnit.value; }); });
					}),
					d3.max(data, function(d) { return d3.max(d.charts, function(c) {
						return d3.max(c.values, function(v) { return v.x / currTimeUnit.value; }); });
					})
				])
				.range([0, width]);
		}
		x = d3.scale.linear()
			.domain([
				d3.min(data, function(d) { return d3.min(d.charts, function(c) {
					return d3.min(c.values, function(v) { return v.x / currTimeUnit.value; }); });
				}),
				d3.max(data, function(d) { return d3.max(d.charts, function(c) {
					return d3.max(c.values, function(v) { return v.x / currTimeUnit.value; }); });
				})
			])
			.range([0, width]);
		var y = d3.scale.linear()
			.domain([
				d3.min(data, function(d) { return d3.min(d.charts, function(c) {
					return d3.min(c.values, function(v) { return v.y; }); });
				}),
				d3.max(data, function(d) { return d3.max(d.charts, function(c) {
					return d3.max(c.values, function(v) { return v.y; }); });
				})
			])
			.nice()
			.range([height, 0]);
		//
		var color = d3.scale.ordinal()
			.domain(data.map(function(d) { return d.loadType; }))
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
		var svg = d3.select(path)
			.append("div")
			.attr("class", "svg-container")
			.append("svg")
			.attr("width", width + margin.left + margin.right)
			.attr("height", height + margin.top + margin.bottom + 40)
			.append("g")
			.attr("transform", "translate(" + margin.left + "," + margin.top + ")")
			.style("font", "12px sans-serif")
			.style("overflow-x", "auto");

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

		var loadType;

		var levels = svg.selectAll(".level")
			.data(data).enter()
			.append("g")
			.attr("stroke", function(d) { return color(d.loadType); })
			.attr("class", "level")
			.attr("id", function(d) { return path.replace("#", "") + d.loadType; })
			.selectAll("path")
			.data(function(d) {
				loadType = d.loadType;
				return d.charts;
			}).enter()
			.append("path")
			.attr("fill", "none")
			.attr("class", "line")
			.attr("d", function(c) { return line(c.values); })
			.attr("stroke-dasharray", function(c, i) {
				switch (c.name.id) {
					case constants.getAvgConstant().id:
						return "0,0";
					case constants.getLastConstant().id:
						return "3,3";
					case constants.getMinConstant().id:
						return "6,6";
					case constants.getMaxConstant().id:
						return "12,12";
				}
			})
			.attr("id", function(c) {
				return path.replace("#", "") + loadType + "-" + c.name.id;
			})
			.attr("visibility", function(c) { if (c.name.id === constants.getAvgConstant().id) {
				return "visible"; } else { return "hidden";
			}});
		//
		svg.selectAll(".right-foreign")
			.data(data).enter()
			.append("foreignObject")
			.attr("class", "foreign right-foreign")
			.attr("x", width + 3)
			.attr("width", 18)
			.attr("height", 18)
			.attr("transform", function(d, i) {
				return "translate(0," + i * 20 + ")";
			})
			.append("xhtml:body")
			.append("input")
			.attr("type", "checkbox")
			.attr("checked", "checked")
			.on("click", function(d, i) {
				var element = $(path + d.loadType);
				if ($(this).is(":checked")) {
					element.css("opacity", "1")
				} else {
					element.css("opacity", "0");
				}
			});
		//
		var modes;
		if(isLatencyChart) {
			modes = LAT_MODES;
		} else {
			modes = TP_MODES;
		}
		svg.selectAll(".bottom-foreign")
			.data(modes).enter()
			.append("foreignObject")
			.attr("class", "foreign bottom-foreign")
			.attr("width", 18)
			.attr("height", 18)
			.attr("transform", function(d, i) {
				return "translate(" + (i*210 + 20) + "," + (height + (margin.bottom/2) + 24) + ")";
			})
			.append("xhtml:body")
			.append("input")
			.attr("type", "checkbox")
			.attr("class", "bottom-checkbox")
			.attr("value", function(d) { return d.id; })
			.attr("checked", function(d) { if (d.id === constants.getAvgConstant().id) { return "checked"; } })
			.on("click", function(d, i) {
				var currentVal = $(this).val();
				var elements = $(path + " " + ".line");
				if ($(this).is(":checked")) {
					elements.each(function() {
						var splittedString = $(this).attr("id").split("-");
						if (splittedString[splittedString.length - 1] === currentVal) {
							$(this).css("visibility", "visible");
						}
					});
				} else {
					elements.each(function() {
						var splittedString = $(this).attr("id").split("-");
						if (splittedString[splittedString.length - 1] === currentVal) {
							$(this).css("visibility", "hidden");
						}
					});
				}
			});
		//
		var rightLegend = svg.selectAll(".right-legend")
			.data(data).enter()
			.append("g")
			.attr("class", "right-legend")
			.attr("transform", function(d, i) {
				return "translate(0," + i * 20 + ")";
			});

		rightLegend.append("rect")
			.attr("x", width + 21)
			.attr("width", 18)
			.attr("height", 18)
			.style("fill", function(d) { return color(d.loadType); });

		rightLegend.append("text")
			.attr("x", width + 43)
			.attr("y", 9)
			.attr("dy", ".35em")
			.style("text-anchor", "start")
			.text(function(d) { return d.loadType; });
		//
		var bottomLegend = svg.selectAll(".bottom-legend")
			.data(modes).enter()
			.append("g")
			.attr("class", "bottom-legend")
			.attr("stroke", "black")
			.attr("transform", function(d, i) {
				return "translate(" + i*210 + "," + (height + (margin.bottom/2) + 20) + ")";
			});

		bottomLegend.append("path")
			.attr("d", function(d, i) {
				switch(d.id) {
					case constants.getAvgConstant().id:
						return "M20 0 L110 0";
					case constants.getLastConstant().id:
						return "M20 0 L115 0";
					case constants.getMinConstant().id:
						return "M20 0 L120 0";
					case constants.getMaxConstant().id:
						return "M20 0 L125 0";
				}
			})
			.attr("stroke-dasharray", function(d, i) {
				switch (d.id) {
					case constants.getAvgConstant().id:
						return "0,0";
					case constants.getLastConstant().id:
						return "3,3";
					case constants.getMinConstant().id:
						return "6,6";
					case constants.getMaxConstant().id:
						return "12,12"
				}
			});
		bottomLegend.append("text")
			.attr("x", 35)
			.attr("y", 15)
			.attr("dy", ".35em")
			.style("text-anchor", "start")
			.attr("stroke", "none")
			.attr("stroke-width", "none")
			.text(function(d) { return d.text; });
		//  Axis X Label
		var horizontalLabel = svg.append("text")
			.attr("x", (width + margin.left)/2)
			.attr("y", height + margin.top)
			.style("text-anchor", "end")
			.text(xAxisLabel);

		//  Axis Y Label
		svg.append("text")
			.attr("transform", "rotate(-90)")
			.attr("y", -50)
			.attr("x", -(height/2) + 30)
			.attr("dy", ".71em")
			.style("text-anchor", "end")
			.text(yAxisLabel);
		//
		svg.append("text")
			.attr("x", (width / 2))
			.attr("y", 0 - (margin.top / 2))
			.attr("text-anchor", "middle")
			.style("font-size", "16px")
			.style("text-decoration", "underline")
			.text(chartTitle);
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
		d3.select(path)
			.append("a")
			.text("Save chart")
			.style("cursor", "pointer")
			.style("margin-left", margin.left + "px")
			.on("click", function() {
				saveChart(path, 1070, 500);
			});
		//
		function redraw(currXScale, currYScale) {
			switch (currXScale) {
				case scaleTypes[0]:
					xAxisGroup.transition().call(d3.svg.axis().scale(x));
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
			//
			//  Update old charts
			var paths = svg.selectAll(".level")
				.data(data)
				.selectAll("path")
				.data(function(d) {
					return d.charts;
				})
				.attr("d", function(c) { return line(c.values); })
				.attr("stroke-dasharray", function(c, i) {
					switch (c.name.id) {
						case constants.getAvgConstant().id:
							return "0,0";
						case constants.getLastConstant().id:
							return "3,3";
						case constants.getMinConstant().id:
							return "6,6";
						case constants.getMaxConstant().id:
							return "12,12";
					}
				})
				.attr("fill", "none");

			//  Test
			var rightLegend = svg.selectAll(".right-legend")
				.data(data).enter()
				.append("g")
				.attr("class", "right-legend")
				.attr("transform", function(d, i) {
					return "translate(0," + i * 20 + ")";
				});

			rightLegend.append("rect")
				.attr("x", width + 21)
				.attr("width", 18)
				.attr("height", 18)
				.style("fill", function(d) { return color(d.loadType); });

			rightLegend.append("text")
				.attr("x", width + 43)
				.attr("y", 9)
				.attr("dy", ".35em")
				.style("text-anchor", "start")
				.text(function(d) { return d.loadType; });
			//
			svg.selectAll(".right-foreign")
				.data(data).enter()
				.append("foreignObject")
				.attr("class", "foreign right-foreign")
				.attr("x", width + 3)
				.attr("width", 18)
				.attr("height", 18)
				.attr("transform", function(d, i) {
					return "translate(0," + i * 20 + ")";
				})
				.append("xhtml:body")
				.append("input")
				.attr("type", "checkbox")
				.attr("checked", "checked")
				.on("click", function(d, i) {
					var element = $(path + d.loadType);
					if ($(this).is(":checked")) {
						element.css("opacity", "1")
					} else {
						element.css("opacity", "0");
					}
				});
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
		//
		common.appendScaleLabels(svg, {
			updateScales: function(scaleOrientation, scaleType) {
				// find scale and apply params to it
				if (scaleType === scaleTypes[0]) {
					if (scaleOrientation === scalesOrientation[0]) {
						x = d3.scale.linear()
							.domain([
								d3.min(data, function(d) { return d3.min(d.charts, function(c) {
									return d3.min(c.values, function(v) { return v.x / currTimeUnit.value; }); });
								}),
								d3.max(data, function(d) { return d3.max(d.charts, function(c) {
									return d3.max(c.values, function(v) { return v.x / currTimeUnit.value; }); });
								})
							])
							.range([0, width]);
						currXScale = scaleTypes[0];
					} else {
						y = d3.scale.linear()
							.domain([
								d3.min(data, function(d) { return d3.min(d.charts, function(c) {
									return d3.min(c.values, function(v) { return v.y; }); });
								}),
								d3.max(data, function(d) { return d3.max(d.charts, function(c) {
									return d3.max(c.values, function(v) { return v.y; }); });
								})
							])
							.nice()
							.range([height, 0]);
						currYScale = scaleTypes[0];
					}
				} else {
					if (scaleOrientation === scalesOrientation[0]) {
						x = d3.scale.log()
							.domain([
								d3.min(data, function(d) { return d3.min(d.charts, function(c) {
									return d3.min(c.values, function(v) {
										return (v.x <= 0) ? (0.1 / currTimeUnit.value)
											: (v.x / currTimeUnit.value);
									}); });
								}),
								d3.max(data, function(d) { return d3.max(d.charts, function(c) {
									return d3.max(c.values, function(v) {
										return (v.x <= 0) ? (0.1 / currTimeUnit.value)
											: (v.x / currTimeUnit.value);
									}); });
								})
							])
							.range([0, width]);
						currXScale = scaleTypes[1];
					} else {
						y = d3.scale.log()
							.domain([
								d3.min(data, function(d) { return d3.min(d.charts, function(c) {
									return d3.min(c.values, function(v) { return (v.y <= 0) ? 0.001 : v.y; }); });
								}),
								d3.max(data, function(d) { return d3.max(d.charts, function(c) {
									return d3.max(c.values, function(v) { return (v.y <= 0) ? 0.001 : v.y; }); });
								})
							])
							.nice()
							.range([height, 0]);
						currYScale = scaleTypes[1];
					}
				}
				redraw(currXScale, currYScale);
			}
		}, 50);
		return function(chartType, json) {
			json.threadName = json.threadName.match(common.getThreadNamePattern())[0];
			var loadType = json.threadName;
			//
			var parsedValue = common.parsePerfAvgLogEvent(chartType, json.message.formattedMessage);
			//
			var isFound = false;
			data.forEach(function(d) {
				if (d.loadType === loadType) {
					isFound = true;
					d.currentRunMetricsPeriodSec += parseInt(metricsSec);
					d.charts.forEach(function(c, i) {
						if (c.values.length === constants.getPointsOnChartLimit()) {
							c.values = vis.simplify(c.values);
						}
						c.values.push({x: d.currentRunMetricsPeriodSec, y: parseFloat(parsedValue[i])});
					})
				}
			});
			if (!isFound) {
				//currentMetricsPeriodSec = 0;
				//
				var d = {
					loadType: loadType,
					charts: [
						{
							name: constants.getAvgConstant(),
							values: [
								{x: 0, y: 0}
							]
						}, {
							name: constants.getLastConstant(),
							values: [
								{x: 0, y: 0}
							]
						}
					],
					currentRunMetricsPeriodSec: 0
				};
				//
				if(isLatencyChart) {
					d = {
						loadType: loadType,
						charts: [
							{
								name: constants.getAvgConstant(),
								values: [
									{x: 0, y: 0}
								]
							}, {
								name: constants.getMinConstant(),
								values: [
									{x: 0, y: 0}
								]
							}, {
								name: constants.getMaxConstant(),
								values: [
									{x: 0, y: 0}
								]
							}
						],
						currentRunMetricsPeriodSec: 0
					};
				}
				data.push(d);

				var levels = svg.selectAll(".level")
					.data(data).enter()
					.append("g")
					.attr("stroke", function(d) { return color(d.loadType); })
					.attr("class", "level")
					.attr("id", function(d) { return path.replace("#", "") + d.loadType; })
					.selectAll("path")
					.data(function(d) {
						loadType = d.loadType;
						return d.charts;
					}).enter()
					.append("path")
					.attr("class", "line")
					.attr("d", function(c) { return line(c.values); })
					.attr("stroke-dasharray", function(c, i) {
						return i*15 + "," + i*15;
					})
					.attr("id", function(c) { return path.replace("#", "") + loadType + "-" + c.name.id; })
					.attr("visibility", function(c) {
						var elements = $(path + " " + ".bottom-checkbox:checked");
						var isFound = false;
						elements.each(function() {
							if (c.name.id === $(this).val()) {
								isFound = true;
							}
						});
						if (isFound) {
							return "visible";
						}
						return "hidden";
					});
			}
			//
			while (common.isTimeLimitReached(x.domain()[x.domain().length - 1], currTimeUnit)) {
				if (currTimeUnit.next === null) {
					return;
				}
				currTimeUnit = timeLimit[currTimeUnit.next];
				horizontalLabel.text(currTimeUnit.label);
				x.domain([
					d3.min(data, function(d) { return d3.min(d.charts, function(c) {
						return d3.min(c.values, function(v) {
							return (isNaN(x(v.x))) ? (0.1 / currTimeUnit.value)
								: (v.x / currTimeUnit.value);
						}); });
					}),
					d3.max(data, function(d) { return d3.max(d.charts, function(c) {
						return d3.max(c.values, function(v) {
							return (isNaN(x(v.x))) ? (0.1 / currTimeUnit.value)
								: (v.x / currTimeUnit.value);
						}); });
					})
				]);
			}
			//
			x.domain([
				d3.min(data, function(d) { return d3.min(d.charts, function(c) {
					return d3.min(c.values, function(v) {
						return (isNaN(x(v.x))) ? (0.1 / currTimeUnit.value)
							: (v.x / currTimeUnit.value);
					}); });
				}),
				d3.max(data, function(d) { return d3.max(d.charts, function(c) {
					return d3.max(c.values, function(v) {
						return (isNaN(x(v.x))) ? (0.1 / currTimeUnit.value)
							: (v.x / currTimeUnit.value);
					}); });
				})
			]);
			y.domain([
				d3.min(data, function(d) { return d3.min(d.charts, function(c) {
					return d3.min(c.values, function(v) { return (isNaN(y(v.y))) ? 0.001 : v.y; }); });
				}),
				d3.max(data, function(d) { return d3.max(d.charts, function(c) {
					return d3.max(c.values, function(v) { return (isNaN(y(v.y))) ? 0.001 : v.y; }); });
				})
			])
				.nice();
			//
			redraw(currXScale, currYScale);
		};
	}

	return {
		initDataArray: initDataArray,
		clearArrays: clearArrays,
		drawThroughputChart: drawThroughputChart,
		drawBandwidthChart: drawBandwidthChart,
		drawLatencyChart: drawLatencyChart,
		drawDurationChart: drawDurationChart
	}
});