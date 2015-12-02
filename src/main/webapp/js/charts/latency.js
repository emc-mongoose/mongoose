define(["d3js", "../util/visvalingam"], function(d3, vis) {

	var margin = {top: 40, right: 200, bottom: 80, left: 60},
		width = 1070 - margin.left - margin.right,
		height = 460 - margin.top - margin.bottom;

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
	//
	var RUN_TIME_CONFIG_CONSTANTS = {
		runId: "run.id",
		runMetricsPeriodSec: "load.metricsPeriodSec",
		runScenarioName: "scenario.name"
	};
	//
	var CRITICAL_DOTS_COUNT = 1000;
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
	var AVG = {
			id: "avg",
			text: "total average"
		},
		MIN = {
			id: "min",
			text: "min"
		},
		MAX = {
			id: "max",
			text: "max"
		};

	function drawChart(data, json, xLabel, yLabel, chartDOMPath, sec) {
		//
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
		var xScale = d3.scale.linear()
			.domain([
				d3.min(data, function(c) { return d3.min(c.values, function(d) { return d.x; }); }),
				d3.max(data, function(c) { return d3.max(c.values, function(d) { return d.x; }); })
			])
			.range([0, width]);
		//
		while (isTimeLimitReached(xScale.domain()[xScale.domain().length - 1], currTimeUnit)) {
			if (currTimeUnit.next === null) {
				return;
			}
			currTimeUnit = TIME_LIMITATIONS[currTimeUnit.next];
			xLabel = currTimeUnit.label;
			xScale = d3.scale.linear()
				.domain([
					d3.min(data, function(c) { return d3.min(c.values, function(d) { return d.x / currTimeUnit.value; }); }),
					d3.max(data, function(c) { return d3.max(c.values, function(d) { return d.x / currTimeUnit.value; })  })
				])
				.range([0, width]);
		}
		//
		xScale = d3.scale.linear()
			.domain([
				d3.min(data, function(c) { return d3.min(c.values, function(d) { return d.x / currTimeUnit.value; }); }),
				d3.max(data, function(c) { return d3.max(c.values, function(d) { return d.x / currTimeUnit.value; })  })
			])
			.range([0, width]);
		//
		var yScale = d3.scale.linear()
			.domain([
				d3.min(data, function(c) { return d3.min(c.values, function(d) { return d.y; }); }),
				d3.max(data, function(c) { return d3.max(c.values, function(d) { return d.y; })  })
			])
			.nice()
			.range([height, 0]);
		//  set color scale
		var color = d3.scale.ordinal()
			.domain(data.map(function(d) { return d.id; }))
			.range(predefinedColors);
		//  set input chart domain
		/*var xScale = d3.scale.linear()
			.domain([
				d3.min(data, function(c) { return d3.min(c.values, function(d) { return d.x; }); }),
				d3.max(data, function(c) { return d3.max(c.values, function(d) { return d.x; }); })
			])
			.range([0, width]);
		var yScale = d3.scale.linear()
			.domain([
				d3.min(data, function(c) { return d3.min(c.values, function(d) { return d.y; }); }),
				d3.max(data, function(c) { return d3.max(c.values, function(d) { return d.y; }); })
			])
			.nice()
			.range([height, 0]);*/
		//  set x and y axis
		function makeXAxis() {
			return d3.svg.axis()
				.scale(xScale)
				.orient("bottom");
		}
		function makeYAxis() {
			return d3.svg.axis()
				.scale(yScale)
				.orient("left");
		}
		//  create line generator w/ values from data object
		/*var line = d3.svg.line()
			.x(function(d) {
				return xScale(d.x);
			})
			.y(function(d) {
				return yScale(d.y);
			});*/
		var line = d3.svg.line()
			.x(function (d) {
				return xScale((isNaN(xScale(d.x))) ? (0.1 / currTimeUnit.value)
					: (d.x / currTimeUnit.value));
			})
			.y(function (d) {
				return yScale((isNaN(yScale(d.y))) ? 0.001 : d.y);
			});
		//  draw chart components (grid, axis, e.t.c.)
		var svg = d3.select("body").append("svg")
			.attr("width", width + margin.left + margin.right)
			.attr("height", height + margin.top + margin.bottom)
			.append("g")
				.attr("transform", "translate(" + margin.left + "," + margin.top + ")")
		//  draw axis labels
		var xAxisLabel = svg.append("text")
			.attr("transform",
				"translate(" + (width / 2) + "," + (height + margin.bottom / 2) + ")")
			.style("text-anchor", "middle")
			.text(xLabel);
		var yAxisLabel = svg.append("text")
			.attr("transform", "rotate(-90)")
			.attr("y", -margin.left)
			.attr("x", -(height / 2))
			.attr("dy", "1em")
			.style("text-anchor", "middle")
			.text(yLabel);
		//  draw grid
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
		//  draw x and y axis
		var xAxisGroup = svg.append("g")
			.attr("class", "x axis")
			.attr("transform", "translate(0," + height + ")")
			.call(makeXAxis());
		var yAxisGroup = svg.append("g")
			.attr("class", "y axis")
			.call(makeYAxis());
		//  draw chart title
		svg.append("text")
			.attr("x", (width / 2))
			.attr("y", -(margin.top / 2))
			.attr("text-anchor", "middle")
			.style("font-size", "16px")
			.style("text-decoration", "underline")
			.text(json.threadName);
		//  chart legend
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
			.style("fill", function(d) { return color(d.id); })
			.style("opacity", function(d) { if (d.id == AVG.id) { return 0.8; } else { return 0.2; }})
			.on("click", function(d) {
				//  fix
				var element = d3.select(chartDOMPath + d.id);
				if (element.attr("visibility") == "visible") {
					element.attr("visibility", "hidden");
					d3.select(this).style("opacity", 0.2);

				} else {
					element.attr("visibility", "visible");
					d3.select(this).style("opacity", 0.8);
				}
			});
		legend.append("text")
			.attr("x", width + 40)
			.attr("y", 9)
			.attr("dy", ".35em")
			.style("text-anchor", "start")
			.text(function(d) { return d.label; });
		// it's necessary for save charts

		////////////////////////////////////////////////////////////////////////////////////////////
		//  draw chart curves
		var curves = svg.selectAll(".curves")
			.data(data).enter()
			.append("g")
			.attr("class", "curve")
			.attr("id", function(d) { return chartDOMPath + d.id; })
			//  fix
			.attr("visibility", function(d) {
				if (d.id == "avg") {
					return "visible";
				} else {
					return "hidden";
				}
			})
			.append("path")
			.attr("class", "line")
			.attr("d", function(d)  { return line(d.values); })
			.attr("stroke", function(d) { return color(d.id); })
			.attr("fill", "none");

		//  redraw function
		function redraw(currXScale, currYScale) {
			switch (currXScale) {
				case SCALE_TYPES[0]:
					xAxisGroup.transition().call(d3.svg.axis().scale(xScale).orient("bottom"));
					xGrid.call(d3.svg.axis().scale(xScale)
						.tickSize(-height, 0, 0)
						.tickFormat(""));
					xGrid.selectAll(".minor")
						.style("stroke-opacity", "0.7")
						.style("shape-rendering", "crispEdges")
						.style("stroke", "lightgrey")
						.style("stroke-dasharray", "0");
					break;
				case SCALE_TYPES[1]:
					xAxisGroup.transition().call(d3.svg.axis().scale(xScale).orient("bottom").ticks(0, ".1s"));
					xGrid.call(d3.svg.axis().scale(xScale)
						.tickSize(-height, 0, 0)
						.tickFormat(""))
						.selectAll(".tick")
						.data(xScale.ticks().map(xScale.tickFormat(0, ".1")), function(d) { return d; })
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
					yAxisGroup.transition().call(d3.svg.axis().scale(yScale).orient("left"));
					yGrid.call(d3.svg.axis().scale(yScale).orient("left")
						.tickSize(-width, 0, 0)
						.tickFormat(""));
					yGrid.selectAll(".minor")
						.style("stroke-opacity", "0.7")
						.style("shape-rendering", "crispEdges")
						.style("stroke", "lightgrey")
						.style("stroke-dasharray", "0");
					break;
				case SCALE_TYPES[1]:
					yAxisGroup.transition().call(d3.svg.axis().scale(yScale).orient("left").ticks(0, ".1s"));
					yGrid.call(d3.svg.axis().scale(yScale).orient("left")
						.tickSize(-width, 0, 0)
						.tickFormat(""))
						.selectAll(".tick")
						.data(yScale.ticks().map(yScale.tickFormat(0, ".1")), function(d) { return d; })
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
				.attr("stroke", function(d) { return color(d.id); })
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
		//
		//  checkboxes for linear/log scales
		appendScaleLabels(svg, {
			updateScales: function(scaleOrientation, scaleType) {
				// find scale and apply params to it
				if (scaleType === SCALE_TYPES[0]) {
					if (scaleOrientation === SCALE_ORIENTATION[0]) {
						xScale = d3.scale.linear()
							.domain([d3.min(data, function(c) { return d3.min(c.values, function(d) {
								return d.x / currTimeUnit.value;
							}); }),
								d3.max(data, function(c) { return d3.max(c.values, function(d) {
									return d.x / currTimeUnit.value;
								}); })])
							.range([0, width]);
						currXScale = SCALE_TYPES[0];
					} else {
						yScale = d3.scale.linear()
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
						xScale = d3.scale.log()
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
						yScale = d3.scale.log()
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
			while (isTimeLimitReached(xScale.domain()[xScale.domain().length - 1], currTimeUnit)) {
				if (currTimeUnit.next === null) {
					return;
				}
				currTimeUnit = TIME_LIMITATIONS[currTimeUnit.next];
				xAxisLabel.text(currTimeUnit.label);
				xScale.domain([
					d3.min(data, function(c) { return d3.min(c.values, function(d) {
						return (isNaN(xScale(d.x))) ? (0.1 / currTimeUnit.value)
							: (d.x / currTimeUnit.value);
					}); }),
					d3.max(data, function(c) { return d3.max(c.values, function(d) {
						return (isNaN(xScale(d.x))) ? (0.1 / currTimeUnit.value)
							: (d.x / currTimeUnit.value);
					}); })
				]);
			}
			//
			xScale.domain([
				d3.min(data, function(c) { return d3.min(c.values, function(d) {
					return (isNaN(xScale(d.x))) ? (0.1 / currTimeUnit.value)
						: (d.x / currTimeUnit.value);
				}); }),
				d3.max(data, function(c) { return d3.max(c.values, function(d) {
					return (isNaN(xScale(d.x))) ? (0.1 / currTimeUnit.value)
						: (d.x / currTimeUnit.value);
				}); })
			]);
			yScale.domain([
				d3.min(data, function(c) { return d3.min(c.values, function(d) {
					return (isNaN(yScale(d.y))) ? 0.001 : d.y }); }),
				d3.max(data, function(c) { return d3.max(c.values, function(d) {
					return (isNaN(yScale(d.y))) ? 0.001 : d.y
				}); })
			])
				.nice();
			//
			redraw(currXScale, currYScale);
		};
	}

	function saveChart(chartDOMPath, w, h) {
		var html;
		if (typeof chartDOMPath === 'object') {
			html = d3.select(chartDOMPath)
				.attr("version", 1.1)
				.attr("xmlns", "http://www.w3.org/2000/svg")
				.node().parentNode.innerHTML;
		} else {
			html = d3.select(chartDOMPath + " svg")
				.attr("version", 1.1)
				.attr("xmlns", "http://www.w3.org/2000/svg")
				.node().parentNode.innerHTML;
		}
		var theResult = $.strRemove(".foreign", html);
		//
		var imgSrc = 'data:image/svg+xml;base64,' + btoa(theResult);
		//
		var canvas = document.createElement("canvas");
		//
		canvas.setAttribute("width", w);
		canvas.setAttribute("height", h);
		//
		var context = canvas.getContext("2d");
		//
		var image = new Image();
		image.src = imgSrc;
		image.onload = function() {
			context.drawImage(image, 0, 0, w, h);
			var canvasData = canvas.toDataURL("image/png");
			//
			var a = document.createElement("a");
			a.download = Math.random().toString(36).substring(7) + ".png";
			a.href = canvasData;
			document.body.appendChild(a);
			a.click();
			document.body.removeChild(a);
		};
	}

	function appendScaleLabels(svg, chartEntry, addHeight) {
		var groups = svg.selectAll(".scale-labels")
			.data(SCALES);
		//  enter selection
		var groupsEnter = groups.enter().append("g")
			.attr("class", "scale-labels")
			.attr("name", function(d) { return d.id; })
			.attr("transform", function(d, i) {
				return "translate(" + (10 + i*130)
					+ "," + (height + (margin.bottom/2) + addHeight + 10) + ")";
			});
		groupsEnter.append("text")
			.attr("dy", ".35em")
			.style("text-anchor", "start")
			.text(function(d) {
				return d.id;
			});
		groupsEnter.append("foreignObject")
			.attr("class", "foreign")
			.attr("width", 18)
			.attr("height", 18)
			.attr("transform", "translate(20, -10)")
			.append("xhtml:body")
			.append("input")
			.attr("type", "checkbox")
			.style("margin-left", "4px")
			.on("click", function(d) {
				var currScaleType = null;
				if (d3.select(this).property("checked")) {
					currScaleType = SCALE_TYPES[1];
				} else {
					d3.select(this).property("checked", false);
					currScaleType = SCALE_TYPES[0];
				}
				//  select current checkbox
				var parentGroup = d3.select(this.parentNode.parentNode.parentNode);
				var scaleOrientation = parentGroup.attr("name");
				chartEntry.updateScales(scaleOrientation, currScaleType);
			});
		groupsEnter.append("text")
			.attr("class", "foreign-labels")
			.attr("x", 25)
			.attr("y", 10)
			.attr("dy", ".35em")
			.attr("transform", "translate(20, -10)")
			.style("text-anchor", "start")
			.text(SCALE_TYPES[1]);
	}
	//
	function isTimeLimitReached(domainMaxValue, currTimeUnit) {
		return domainMaxValue >= currTimeUnit.limit;
	}
	//
	return {
		drawChart: drawChart
	};
});