define([
	"d3js",
	"../util/common",
	"../util/visvalingam",
	"../../util/constants"
], function(
	d3, common, vis, constants
) {
	var margin = common.getMargin(),
		width = 480,
		height = common.getHeight();

	function drawThroughputCharts(runId, loadTypes, loadRampupSizesArray, rampupConnCountsArray) {
		//
		var data = [];
		loadTypes.forEach(function(d) {
			data.push({
				"loadType": d.trim(),
				"sizes": (function() {
					var sizesArray = [];
					loadRampupSizesArray.forEach(function(d, i) {
						sizesArray[i] = {
							"size": d + "-" + i,
							"charts": [
								{
									"name": constants.getAvgConstant(),
									"values": []
								}
							]
						}
					});
					return sizesArray;
				})()
			});
		});
		var updateFunction = drawCharts(
			data, "Connection count", "Rate[op/s]", "#tp-" + runId.split(".").join("_"),
				rampupConnCountsArray, loadRampupSizesArray
		);
		return {
			update: function(json) {
				updateFunction(constants.getChartTypes().TP, json);
			}
		};
	}
	//
	function drawBandwidthCharts(runId, loadTypes, loadRampupSizesArray, rampupConnCountsArray) {
		var data = [];
		loadTypes.forEach(function(d) {
			data.push({
				"loadType": d.trim(),
				"sizes": (function() {
					var sizesArray = [];
					loadRampupSizesArray.forEach(function(d, i) {
						sizesArray[i] = {
							"size": d + "-" + i,
							"charts": [
								{
									"name": constants.getAvgConstant(),
									"values": []
								}
							]
						}
					});
					return sizesArray;
				})()
			});
		});
		var updateFunction = drawCharts(
			data, "Connection count", "Rate[MB/s]", "#bw-" + runId.split(".").join("_"),
				rampupConnCountsArray, loadRampupSizesArray
		);
		return {
			update: function(json) {
				updateFunction(constants.getChartTypes().BW, json);
			}
		};
	}

	function drawCharts(data, xAxisLabel, yAxisLabel, path, rampupConnCountsArray, loadRampupSizesArray) {
		var scalesArray = [];
		var scaleTypes = constants.getScaleTypes();
		var scalesOrientation = constants.getScaleOrientations();
		//
		data.forEach(function(d, i) {
			var currIndex = i;
			var currXScale = scaleTypes[0];
			var currYScale = scaleTypes[0];
			//
			var x = d3.scale.linear()
				.domain([0, d3.max(rampupConnCountsArray)])
				.nice()
				.range([0, width]);
			var y = d3.scale.linear()
				.domain([
					0,
					d3.max(d.sizes, function(c) { return d3.max(c.charts, function(v) {
						return d3.max(v.values, function(val) { return val.y; });
					}); })
				])
				.nice()
				.range([height, 0]);
			//
			scalesArray.push({
				xLabel: currXScale,
				yLabel: currYScale,
				x: x,
				y: y,
				update: function(index, x, y) {
					redraw(index, x, y);
				}
			});
			var color = d3.scale.ordinal()
				.domain(loadRampupSizesArray.map(function(d) { return d; }))
				.range(constants.getChartColors());
			//
			var xAxis = d3.svg.axis()
				.scale(x)
				.orient("bottom");
			var yAxis = d3.svg.axis()
				.scale(y)
				.orient("left");
			//
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
			var line = d3.svg.line()
					.x(function (d) {
						return x((isNaN(x(d.x))) ? 0.1 : d.x);
					})
					.y(function (d) {
						return y((isNaN(y(d.y))) ? 0.001 : d.y);
					});
			var svg = d3.select(path)
					.append("div")
					.attr("class", "svg-container")
					.append("svg")
					.attr("width", width + (margin.left) + margin.right)
					.attr("height", height + margin.top + margin.bottom)
					.attr("id", path.replace("#", "") + "-" + d.loadType)
					.append("g")
					.attr("transform", "translate(" + (margin.left + 30) + "," + margin.top + ")")
					.style("font", "10px sans-serif")
					.style("overflow-x", "auto");
			//
			var xAxisGroup = svg.append("g")
					.attr("class", "axis x-axis")
					.attr("transform", "translate(0," + height + ")")
					.call(xAxis);
			var yAxisGroup = svg.append("g")
					.attr("class", "axis y-axis")
					.call(yAxis);
			var xGrid = svg.append("g")
					.attr("class", "grid x-grid")
					.attr("transform", "translate(0," + height + ")")
					.call(makeXAxis()
							.tickSize(-height, 0, 0)
							.tickFormat(""));
			var yGrid = svg.append("g")
					.attr("class", "grid y-grid")
					.call(makeYAxis()
							.tickSize(-width, 0, 0)
							.tickFormat(""));
			//  Axis X Label
			svg.append("text")
					.attr("x", (width + margin.left)/2)
					.attr("y", height + margin.top - 8)
					.style("text-anchor", "end")
					.text(xAxisLabel);
			//  Axis Y Label
			svg.append("text")
					.attr("transform", "rotate(-90)")
					.attr("y", -80)
					.attr("x", -(height/2) + 30)
					.attr("dy", ".71em")
					.style("text-anchor", "end")
					.text(yAxisLabel);
			svg.append("text")
					.attr("x", (width / 2))
					.attr("y", 0 - (margin.top / 2))
					.attr("text-anchor", "middle")
					.style("font-size", "16px")
					.style("text-decoration", "underline")
					.text(d.loadType);
			//
			var legend = svg.selectAll(".legend")
					.data(loadRampupSizesArray).enter()
					.append("g")
					.attr("class", "legend")
					.attr("transform", function(d, i) {
						return "translate(0," + i * 20 + ")";
					});

			legend.append("rect")
					.attr("x", width + 20)
					.attr("width", 18)
					.attr("height", 18)
					.style("fill", function(d) { return color(d); });

			legend.append("text")
					.attr("x", width + 42)
					.attr("y", 9)
					.attr("dy", ".35em")
					.style("text-anchor", "start")
					.text(function(d) { return d; });
			//
			var levels = svg.selectAll(".level")
					.data(d.sizes).enter()
					.append("g")
					.attr("class", "level")
					.attr("id", function(c, i) { return path.replace("#", "") + "-" + d.loadType + "-" + c.size + "-" + i; })
					.attr("fill", function(c, i) { return color(loadRampupSizesArray[i]); })
					.attr("stroke", function(c, i) { return color(loadRampupSizesArray[i]); })
					.selectAll("path")
					.data(function(c) { return c.charts; }).enter()
					.append("path")
					.attr("class", "line")
					.attr("d", function(v) { return line(v.values); });
			svg.selectAll(".right-foreign")
					.data(d.sizes).enter()
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
					.attr("value", function(d) { return d.size; })
					.attr("checked", "checked")
					.on("click", function(c, i) {
						var element = $(path + "-" + d.loadType + "-" + c.size + "-" + i);
						if ($(this).is(":checked")) {
							element.css("opacity", "1");
						} else {
							element.css("opacity", "0");
						}
					});
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
						saveChart(svg.node().parentNode, 740, 460);
					});
			function redraw(index, x, y) {
				var currXScale = scalesArray[index].xLabel;
				var currYScale = scalesArray[index].yLabel;
				//
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
				//
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
				var loadTypeSvg = d3.select(path + "-" + d.loadType);
				var currentLoadType = d.loadType;
				//
				d.sizes.forEach(function(d, i) {
					//
					var paths = loadTypeSvg.select(path + "-" + currentLoadType + "-" + d.size + "-" + i)
							.selectAll("path").data(d.charts)
							.attr("d", function(v) { return line(v.values); })
							.attr("fill", "none");
					//
					var dots = loadTypeSvg.select(path + "-" + currentLoadType + "-" + d.size + "-" + i)
							.data(d.charts)
							.selectAll(".dot").data(function(v) { return v.values; })
							.enter().append("circle")
							.attr("class", "dot")
						//.style("stroke-width", "1.5px")
							.attr("cx", function(coord) { return x((isNaN(x(coord.x))) ? 0.1 : coord.x); })
							.attr("cy", function(coord) { return y((isNaN(y(coord.y))) ? 0.001 : coord.y); })
							.attr("r", 2);
					//  Update dots
					loadTypeSvg.select(path + "-" + currentLoadType + "-" + d.size + "-" + i)
							.selectAll(".dot").data(function(v) { return v.values; })
							.attr("cx", function(coord) { return x((isNaN(x(coord.x))) ? 0.1 : coord.x); })
							.attr("cy", function(coord) { return y((isNaN(y(coord.y))) ? 0.001 : coord.y); })
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
			}
			//
			common.appendScaleLabels(svg, {
				updateScales: function(scaleOrientation, scaleType) {
					// find scale and apply params to it
					if (scaleType === scaleTypes[0]) {
						if (scaleOrientation === scalesOrientation[0]) {
							x = d3.scale.linear()
									.domain([d3.min(rampupConnCountsArray), d3.max(rampupConnCountsArray)])
									.nice()
									.range([0, width]);
							currXScale = scaleTypes[0];
						} else {
							y = d3.scale.linear()
									.domain([
										0,
										d3.max(d.sizes, function(c) { return d3.max(c.charts, function(v) {
											return d3.max(v.values, function(val) { return val.y; });
										}); })
									])
									.nice()
									.range([height, 0]);
							currYScale = scaleTypes[0];
						}
					} else {
						if (scaleOrientation === scalesOrientation[0]) {
							x = d3.scale.log()
									.domain([d3.min(rampupConnCountsArray), d3.max(rampupConnCountsArray)])
									.nice()
									.range([0, width]);
							currXScale = scaleTypes[1];
						} else {
							y = d3.scale.log()
									.domain([
										d3.min(d.sizes, function(c) { return d3.min(c.charts, function(v) {
											return d3.min(v.values, function(val) {
												return (val.y <= 0) ? 0.001 : val.y;
											});
										}); }),
										d3.max(d.sizes, function(c) { return d3.max(c.charts, function(v) {
											return d3.max(v.values, function(val) {
												return (val.y <= 0) ? 0.001 : val.y;
											});
										}); })
									])
									.nice()
									.range([height, 0]);
							currYScale = scaleTypes[1];
						}
					}
					//
					scalesArray[currIndex].x = x;
					scalesArray[currIndex].xLabel = currXScale;
					scalesArray[currIndex].y = y;
					scalesArray[currIndex].yLabel = currYScale;
					redraw(currIndex, x, y);
				}
			}, 10);
		});
		//
		return function(chartType, json, array) {
			var isFound = false;
			data.forEach(function(d, i) {
				if (json.message.formattedMessage.split(" ")[0].slice(1, -1).toLowerCase().indexOf(d.loadType) > -1) {
					var x = scalesArray[i].x;
					var y = scalesArray[i].y;
					var currYScale = scalesArray[i].yLabel;
					var loadTypeSvg = d3.select(path + "-" + d.loadType);
					//
					var currentLoadType = d.loadType;
					var currentSizes = d.sizes;
					//
					var parsedValue = common.parsePerfAvgLogEvent(chartType, json.message.formattedMessage);
					//
					d.sizes.forEach(function (d, i) {
						if (d.size === json.contextMap["currentSize"]) {
							d.charts.forEach(function (c, i) {
								c.values.push({
									x: parseInt(json.contextMap["currentConnCount"]),
									y: parseFloat(parsedValue[i])
								});
							});
						}
						//
					});
					//
					x
						.domain([d3.min(rampupConnCountsArray), d3.max(rampupConnCountsArray)])
						.nice()
						.range([0, width]);
					y.domain([
						(currYScale !== scaleTypes[1]) ? 0 : d3.min(currentSizes, function (c) {
							return d3.min(c.charts, function (v) {
								return d3.min(v.values, function (val) {
									return (val.y <= 0) ? 0.001 : val.y;
								});
							});
						}),
						d3.max(currentSizes, function (c) {
							return d3.max(c.charts, function (v) {
								return d3.max(v.values, function (val) {
									return ((currYScale === scaleTypes[1]) && val.y <= 0) ?
											0.001 : val.y;
								});
							});
						})
					]).nice().range([height, 0]);
					//
					scalesArray[i].update(i, x, y);
				}
			});
		}
	}

	return {
		drawThroughputCharts: drawThroughputCharts,
		drawBandwidthCharts: drawBandwidthCharts
	};
});
