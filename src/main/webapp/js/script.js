require(["./requirejs/conf"], function() {
	require(["d3js", "bootstrap", "./util/visvalingam"], function(d3, bootstrap, vis) {
		//  Charts
		function charts(chartsArray) {
			var margin = {top: 40, right: 200, bottom: 60, left: 60},
				width = 1070 - margin.left - margin.right,
				height = 460 - margin.top - margin.bottom;
			//
			var SCENARIO = {
				single: "single",
				chain: "chain",
				rampup: "rampup"
			};
			var CHART_TYPES = {
				TP: "throughput",
				BW: "bandwidth"
			};
			var AVG = {
					id: "avg",
					text: "total average"
				},
				LAST = {
					id: "last",
					text: "last 10 sec"
				};
			var SCALE_TYPES = ["Linear Scale", "Log Scale"];
			var SCALE_ORIENTATION = ["x", "y"];
			//
			var SCALES = [
				{
					id: "x",
					types: SCALE_TYPES
				}, {
					id: "y",
					types: SCALE_TYPES
				}
			];
			//  for time accomodation. For more info see #JIRA-314
			var TIME_LIMITATIONS = {
				"seconds": {
					"limit": 300,
					"value": 1,
					"next": "minutes",
					"label": "t[seconds]"
				},
				"minutes": {
					"limit": 300,
					"value": 60,
					"next": "hours",
					"label": "t[minutes]"
				},
				"hours": {
					"limit": 120,
					"value": 60 * 60,
					"next": "days",
					"label": "t[hours]"
				},
				"days": {
					"limit": 35,
					"value": 24 * 60 * 60,
					"next": "weeks",
					"label": "t[days]"
				},
				"weeks": {
					"limit": 20,
					"value": 7 * 24 * 60 * 60,
					"next": "months",
					"label": "t[weeks]"
				},
				"months": {
					"limit": 60,
					"value": 4 * 7 * 24 * 60 * 60,
					"next": "years",
					"label": "t[months]"
				},
				"years": {
					"next": null,
					"label": "t[years]"
				}
			};
			//  Some constants from runTimeConfig
			var RUN_TIME_CONFIG_CONSTANTS = {
				runId: "run.id",
				runMetricsPeriodSec: "load.metricsPeriodSec",
				runScenarioName: "scenario.name"
			};
			//
			var CRITICAL_DOTS_COUNT = 1000;
			//
			var predefinedColors = [
				// primary
				"#0000ff", // b
				"#007f00", // g
				"#c00000", // r
				// secondary
				"#00c0c0", // c
				"#c000c0", // m
				"#c0c000", // y
				// tertiary
				"#00c07f", // spring green
				"#7fc000", // chartreuse
				"#c07f00", // orange
				"#c0007f", // rose
				"#7f00c0", // violet
				"#007fc0" // azure
			];
			var CHART_MODES = [AVG, LAST];
			//  Common functions for chart
			//
			function simplifyChart(destArray, dataArray, chartType, runMetricsPeriodSec) {
				var currMetricsPeriodSec = -runMetricsPeriodSec;
				dataArray.forEach(function(d) {
					var value = parsePerfAvgLogEvent(chartType, d.message.formattedMessage);
					currMetricsPeriodSec += runMetricsPeriodSec;
					destArray.forEach(function(item, i) {
						if (item.values.length === CRITICAL_DOTS_COUNT) {
							item.values = vis.simplify(item.values);
						}
						item.values.push({x: currMetricsPeriodSec, y: parseFloat(value[i])});
					});
				});
				return currMetricsPeriodSec;
			}
			//
			function drawThroughputCharts(data, json, sec) {
				var updateFunction = drawChart(data, json, "t[seconds]", "Rate[op/s]",
					"#tp-" + json.contextMap[RUN_TIME_CONFIG_CONSTANTS.runId].split(".").join("_"),
					sec);
				return {
					update: function(json) {
						updateFunction(CHART_TYPES.TP, json.message.formattedMessage);
					}
				};
			}
			//
			function drawBandwidthCharts(data, json, sec) {
				var updateFunction = drawChart(data, json, "t[seconds]", "Rate[MB/s]",
					"#bw-" + json.contextMap[RUN_TIME_CONFIG_CONSTANTS.runId].split(".").join("_"),
					sec);
				return {
					update: function(json) {
						updateFunction(CHART_TYPES.BW, json.message.formattedMessage);
					}
				};
			}
			//
			function parsePerfAvgLogEvent(chartType, value) {
				var result = null;
				switch(chartType) {
					case CHART_TYPES.TP:
						var tpPattern = "[\\s]+TP\\[op/s]=\\(([\\.\\d]+)/([\\.\\d]+)\\);";
						var tpArray = value.match(tpPattern);
						result = tpArray.slice(1, tpArray.length);
						break;
					case CHART_TYPES.BW:
						var bwPattern = "[\\s]+BW\\[MB/s]=\\(([\\.\\d]+)/([\\.\\d]+)\\)";
						var bwArray = value.match(bwPattern);
						result = bwArray.slice(1, bwArray.length);
						break;
				}
				//
				return result;
			}
			//
			function isTimeLimitReached(domainMaxValue, currTimeUnit) {
				return domainMaxValue >= currTimeUnit.limit;
			}
			//
			function drawChart(data, json, xAxisLabel, yAxisLabel, chartDOMPath, sec) {
				var currXScale = SCALE_TYPES[0];
				var currYScale = SCALE_TYPES[0];
				//  get some fields from runTimeConfig
				var runMetricsPeriodSec = json.contextMap[RUN_TIME_CONFIG_CONSTANTS.runMetricsPeriodSec];
				//
				json.threadName = json.threadName.match(getThreadNamePattern())[0];
				//
				var currentMetricsPeriodSec = 0;
				if (sec !== undefined) {
					currentMetricsPeriodSec = sec;
				}
				//var runScenarioName = json.contextMap[RUN_TIME_CONFIG_CONSTANTS.runScenarioName];
				//
				var currTimeUnit = TIME_LIMITATIONS.seconds;
				//
				var x = d3.scale.linear()
					.domain([
						d3.min(data, function(c) { return d3.min(c.values, function(d) { return d.x; }); }),
						d3.max(data, function(c) { return d3.max(c.values, function(d) { return d.x; }); })
					])
					.range([0, width]);
				//
				while (isTimeLimitReached(x.domain()[x.domain().length - 1], currTimeUnit)) {
					if (currTimeUnit.next === null) {
						return;
					}
					currTimeUnit = TIME_LIMITATIONS[currTimeUnit.next];
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
					.range(predefinedColors);
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
					.attr("visibility", function(d) { if (d.name.id === AVG.id) { return "visible"; } else { return "hidden"; }})
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
					.attr("checked", function(d) { if (d.name.id === AVG.id) { return "checked"; } })
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
						case SCALE_TYPES[0]:
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
						case SCALE_TYPES[1]:
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
						case SCALE_TYPES[0]:
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
						case SCALE_TYPES[1]:
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
				appendScaleLabels(svg, {
					updateScales: function(scaleOrientation, scaleType) {
						// find scale and apply params to it
						if (scaleType === SCALE_TYPES[0]) {
							if (scaleOrientation === SCALE_ORIENTATION[0]) {
								x = d3.scale.linear()
									.domain([d3.min(data, function(c) { return d3.min(c.values, function(d) {
										return d.x / currTimeUnit.value;
									}); }),
										d3.max(data, function(c) { return d3.max(c.values, function(d) {
											return d.x / currTimeUnit.value;
										}); })])
									.range([0, width]);
								currXScale = SCALE_TYPES[0];
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
								currYScale = SCALE_TYPES[0];
							}
						} else {
							if (scaleOrientation === SCALE_ORIENTATION[0]) {
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
								currXScale = SCALE_TYPES[1];
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
								currYScale = SCALE_TYPES[1];
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
						saveChart(chartDOMPath, 1070, 460);
					});
				return function(chartType, value) {
					currentMetricsPeriodSec += parseInt(runMetricsPeriodSec);
					//
					var parsedValue = parsePerfAvgLogEvent(chartType, value);
					//
					data.forEach(function(d, i) {
						if (d.values.length === CRITICAL_DOTS_COUNT) {
							d.values = vis.simplify(d.values);
						}
						d.values.push({x: currentMetricsPeriodSec, y: parseFloat(parsedValue[i])});
					});

					//
					while (isTimeLimitReached(x.domain()[x.domain().length - 1], currTimeUnit)) {
						if (currTimeUnit.next === null) {
							return;
						}
						currTimeUnit = TIME_LIMITATIONS[currTimeUnit.next];
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
			//
			return {
				single: function(json, array) {
					//  get some fields from runTimeConfig
					var runId = json.contextMap[RUN_TIME_CONFIG_CONSTANTS.runId];
					var runScenarioName = json.contextMap[RUN_TIME_CONFIG_CONSTANTS.runScenarioName];
					var runMetricsPeriodSec =
						parseInt(json.contextMap[RUN_TIME_CONFIG_CONSTANTS.runMetricsPeriodSec]);
					//
					LAST.text = "last " + runMetricsPeriodSec + " sec";
					//
					var data = [
						{
							name: AVG,
							values: [
								{x: 0, y: 0}
							]
						}, {
							name: LAST,
							values: [
								{x: 0, y: 0}
							]
						}
					];
					//
					var throughput = $.extend(true, [], data);
					var bandwidth = $.extend(true, [], data);
					//
					function initializeDataArray(destArray, dataArray, chartType) {
						return simplifyChart(destArray, dataArray, chartType, runMetricsPeriodSec);
					}
					//
					function clearArrays() {
						throughput.forEach(function(d) {
							return d.values.shift();
						});
						bandwidth.forEach(function(d) {
							return d.values.shift();
						})
					}
					//
					if ((array !== undefined) && (array.length > 0)) {
						clearArrays();
						var tpSec = initializeDataArray(throughput, array, CHART_TYPES.TP);
						var bwSec = initializeDataArray(bandwidth, array, CHART_TYPES.BW);
						chartsArray.push(getScenarioChartObject(runId, runScenarioName,
							[drawThroughputCharts(throughput, json, tpSec),
								drawBandwidthCharts(bandwidth, json, bwSec)]));
					} else {
						//
						chartsArray.push(getScenarioChartObject(runId, runScenarioName,
							[drawThroughputCharts(throughput, json),
								drawBandwidthCharts(bandwidth, json)]));
					}
				},
				chain: function(runId, runMetricsPeriodSec, loadType, array) {
					//
					var AVG = {
							id: "avg",
							text: "total average"
						},
						LAST = {
							id: "last",
							text: "last " + runMetricsPeriodSec + " sec"
						};
					//
					var TP_MODES = [AVG, LAST];
					//
					var CHART_TYPES = {
						TP: "throughput",
						BW: "bandwidth"
					};
					//
					var data = [
						{
							loadType: loadType,
							charts: [
								{
									name: AVG,
									values: [
										{x: 0, y: 0}
									]
								}, {
									name: LAST,
									values: [
										{x: 0, y: 0}
									]
								}
							],
							currentRunMetricsPeriodSec: 0
						}
					];
					//
					var throughput = $.extend(true, [], data);
					var bandwidth = $.extend(true, [], data);
					//
					function initializeDataArray(destArray, dataArray, chartType) {
						//
						var loadTypes = [];
						dataArray.filter(function(d) {
							var loadType = d.threadName.match(getThreadNamePattern())[0];
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
							var value = parsePerfAvgLogEvent(chartType, d.message.formattedMessage);
							var loadType = d.threadName.match(getThreadNamePattern())[0];
							destArray.forEach(function(d) {
								if (d.loadType === loadType) {
									isFound = true;
									d.currentRunMetricsPeriodSec += parseInt(runMetricsPeriodSec);
									d.charts.forEach(function(c, i) {
										if (c.values.length === CRITICAL_DOTS_COUNT) {
											c.values = vis.simplify(c.values);
										}
										c.values.push({x: d.currentRunMetricsPeriodSec, y: parseFloat(value[i])});
									})
								}
							});
						});
					}
					//
					function clearArrays() {
						throughput.forEach(function(d) {
							d.charts.forEach(function(c) {
								c.values.shift();
							});
							d.currentRunMetricsPeriodSec = -parseInt(runMetricsPeriodSec);
						});
						bandwidth.forEach(function(d) {
							d.charts.forEach(function(c) {
								c.values.shift();
							});
							d.currentRunMetricsPeriodSec = -parseInt(runMetricsPeriodSec);
						});
					}
					//
					if ((array !== undefined) && (array.length > 0)) {
						clearArrays();
						//
						initializeDataArray(throughput, array, CHART_TYPES.TP);
						initializeDataArray(bandwidth, array, CHART_TYPES.BW);
					}
					//
					chartsArray.push({
						"run.id": runId,
						"run.scenario.name": SCENARIO.chain,
						"charts": [
							drawThroughputChart(throughput),
							drawBandwidthChart(bandwidth)
						]
					});
					//
					function drawThroughputChart(data) {
						var updateFunction = drawChart(data, "Throughput[obj/s]", "t[seconds]", "Rate[op/s]", "#tp-" + runId.split(".").join("_"));
						return {
							update: function(json) {
								updateFunction(CHART_TYPES.TP, json);
							}
						};
					}
					//
					function drawBandwidthChart(data) {
						var updateFunction = drawChart(data, "Bandwidth[MB/s]", "t[seconds]", "Rate[MB/s]", "#bw-" + runId.split(".").join("_"));
						return {
							update: function(json) {
								updateFunction(CHART_TYPES.BW, json);
							}
						};
					}
					//

				},
				rampup: function(runId, scenarioChainLoad, rampupConnCounts, loadRampupSizes) {
					//
					// change default width
					width = 480;
					//
					var loadTypes = scenarioChainLoad.split(",");
					var rampupConnCountsArray = rampupConnCounts.split(",").map(function(item) {
						return parseInt(item, 10);
					});
					var loadRampupSizesArray = loadRampupSizes.split(",").map(function(item) {
						return item.trim();
					});
					var AVG = "total average";
					//
					var CHART_TYPES = {
						TP: "throughput",
						BW: "bandwidth"
					};
					//
					var TP_MODES = [AVG];
					//
					chartsArray.push({
						"run.id": runId,
						"run.scenario.name": SCENARIO.rampup,
						"charts": [
							drawThroughputCharts(),
							drawBandwidthCharts()
						]
					});

					function drawThroughputCharts() {
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
													"name": AVG,
													"values": []
												}
											]
										}
									});
									return sizesArray;
								})()
							});
						});
						var updateFunction = drawCharts(data, "Connection count", "Rate[op/s]", "#tp-" + runId.split(".").join("_"));
						return {
							update: function(json) {
								updateFunction(CHART_TYPES.TP, json);
							}
						};
					}
					//
					function drawBandwidthCharts() {
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
													"name": AVG,
													"values": []
												}
											]
										}
									});
									return sizesArray;
								})()
							});
						});
						var updateFunction = drawCharts(data, "Connection count", "Rate[MB/s]", "#bw-" + runId.split(".").join("_"));
						return {
							update: function(json) {
								updateFunction(CHART_TYPES.BW, json);
							}
						};
					}

					function drawCharts(data, xAxisLabel, yAxisLabel, path) {
						var scalesArray = [];
						data.forEach(function(d, i) {
							var currIndex = i;
							var currXScale = SCALE_TYPES[0];
							var currYScale = SCALE_TYPES[0];
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
								.range(predefinedColors);
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
								.attr("width", width + margin.left + margin.right)
								.attr("height", height + margin.top + margin.bottom)
								.attr("id", path.replace("#", "") + "-" + d.loadType)
								.append("g")
								.attr("transform", "translate(" + margin.left + "," + margin.top + ")")
								.style("font", "12px sans-serif")
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
								.attr("y", -57)
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
									case SCALE_TYPES[0]:
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
									case SCALE_TYPES[1]:
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
									case SCALE_TYPES[0]:
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
									case SCALE_TYPES[1]:
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
							appendScaleLabels(svg, {
								updateScales: function(scaleOrientation, scaleType) {
									// find scale and apply params to it
									if (scaleType === SCALE_TYPES[0]) {
										if (scaleOrientation === SCALE_ORIENTATION[0]) {
											x = d3.scale.linear()
												.domain([0, d3.max(rampupConnCountsArray)])
												.nice()
												.range([0, width]);
											currXScale = SCALE_TYPES[0];
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
											currYScale = SCALE_TYPES[0];
										}
									} else {
										if (scaleOrientation === SCALE_ORIENTATION[0]) {
											x = d3.scale.log()
												.domain([1, d3.max(rampupConnCountsArray)])
												.nice()
												.range([0, width]);
											currXScale = SCALE_TYPES[1];
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
											currYScale = SCALE_TYPES[1];
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
									var parsedValue = parsePerfAvgLogEvent(chartType, json.message.formattedMessage);
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
									x.domain([(isNaN(x(0))) ? 1 : 0, d3.max(rampupConnCountsArray)])
										.nice()
										.range([0, width]);
									y.domain([
										(currYScale !== SCALE_TYPES[1]) ? 0 : d3.min(currentSizes, function (c) {
											return d3.min(c.charts, function (v) {
												return d3.min(v.values, function (val) {
													return (val.y <= 0) ? 0.001 : val.y;
												});
											});
										}),
										d3.max(currentSizes, function (c) {
											return d3.max(c.charts, function (v) {
												return d3.max(v.values, function (val) {
													return ((currYScale === SCALE_TYPES[1]) && val.y <= 0) ?
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
				}
			}
		}
	});
});
