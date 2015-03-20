$(document).ready(function() {
	$("#backup-run\\.mode").val($("#run\\.mode").val());
	var WEBSOCKET_URL = "ws://" + window.location.host + "/logs";
	var TABLE_ROWS_COUNT = 100;
	excludeDuplicateOptions();
	//
	var chartsArray = [];
	var shortPropsMap = {};
	var ul = $(".folders");
	//
	walkTreeMap(jsonProps, ul, shortPropsMap);
	$("#run").parent().find("ul").addChild("<li>")
			.addClass("file")
			.append($("<a>", {
				class: "props",
				href: "#" + "run.id",
				text: "id"
			}));
	shortPropsMap["run.id"] = "";
	buildDivBlocksByPropertyNames(shortPropsMap);
	//
	//generatePropertyPage();
	configureWebSocketConnection(WEBSOCKET_URL, TABLE_ROWS_COUNT).connect(chartsArray);
	//
	$("select").each(function() {
		var notSelected = $("option:not(:selected)", this);
		notSelected.each(function() {
			if ($("#" + $(this).val()).is("div")) {
				$("#" + $(this).val()).hide();
			}
		});
	});
	//
	$("#run-modes select").each(function() {
		var valueSelected = this.value;
		var notSelected = $("option:not(:selected)", this);
		notSelected.each(function() {
			if (!$(this).hasClass(valueSelected)) {
				var element = $("." + $(this).val());
				if (element.parents("#base").length) {
					element.hide();
				}
			}
		});
		$("." + valueSelected).show();
	});
	//
	$('#run-modes select').on("change", function() {
		var valueSelected = this.value;
		var notSelected = $("option:not(:selected)", this);
		notSelected.each(function() {
			if (!$(this).hasClass(valueSelected)) {
				var element = $("." + $(this).val());
				if (element.parents("#base").length) {
					element.hide();
				}
			}
		});
		$("." + valueSelected).show();
	});
	//
	$("select").on("change", function() {
		var valueSelected = this.value;
		var notSelected = $("option:not(:selected)", this);
		notSelected.each(function() {
			if ($("#" + $(this).val()).is("div")) {
            	$("#" + $(this).val()).hide();
            }
		});
		if ($("#" + valueSelected).is("div") && !$("#" + valueSelected).hasClass("modal")) {
			$("#" + valueSelected).show();
		}
	});
	//
	$("#config-type").on("change", function() {
		var valueSelected = this.value;
		if (valueSelected === "base") {
			$(".folders").hide();
		} else {
			$(".folders").show();
		}
	});
	//
	$("#run-modes select").on("change", function() {
		var valueSelected = this.value;
		$("#run-mode").val(valueSelected);
	});
	//
	$("#backup-scenario\\.name").on("change", function() {
		var valueSelected = this.value;
		$("#scenario-button").attr("data-target", "#" + valueSelected);
		changeLoadHint(valueSelected);
	});
	//
	function changeLoadHint(value) {
		switch (value) {
			case "backup-single":
				$("#scenario-load").text("Load: [" + $("#scenario\\.type\\.single\\.load input").val() + "]");
				break;
			case "backup-chain":
				$("#scenario-load").text("Load: [" + $("#scenario\\.type\\.chain\\.load input").val() + "]");
			case "backup-rampup":
				$("#scenario-load").text("Load: [" + $("#scenario\\.type\\.chain\\.load input").val() + "]");
				break;
		}
	}
	$("#backup-scenario\\.name").change();
	//
	$("#backup-api\\.name").on("change", function() {
		var valueSelected = this.value;
		$("#api-button").attr("data-target", "#" + valueSelected);
	});
	//
	$("#backup-run\\.mode").on("change", function() {
		var currElement = $(this);
		$("#run\\.mode").val(currElement.val());
	});
	//
	$("#base input, #base select").on("change", function() {
		var currElement = $(this);
		/*if (currElement.parents(".complex").length === 1) {
			var input = $("#backup-run\\.time\\.input").val();
			var select = $("#backup-run\\.time\\.select").val();
			currElement = $("#backup-run\\.time").val(input + "." + select);
		}*/
		//
		var element = $("#" + currElement.attr("data-pointer").replace(/\./g, "\\.") + " input");
		if (currElement.is("select")) {
			var valueSelected = currElement.children("option").filter(":selected").text().trim();
			$('select[data-pointer="'+currElement.attr("data-pointer")+'"]')
					.val(currElement.val());
			if (element) {
				element.val(valueSelected);
			}
		} else {
			$('input[data-pointer="' + currElement.attr("data-pointer") + '"]')
					.val(currElement.val());
			if (element) {
				element.val(currElement.val());
			}
		}
		if ((currElement.attr("id") === "backup-scenario.type.single.load")
			|| (currElement.attr("id") === "backup-scenario.type.chain.load")) {
			changeLoadHint($("#backup-scenario\\.name").val());
		}
	});
	//
	$("#extended input").on("change", function() {
		/*if ($(this).attr("id") === "run.time") {
			var splittedTimeString = $(this).val().split(".");
			$("#backup-run\\.time\\.input").val(splittedTimeString[0]);
			$("#backup-run\\.time\\.select").val(splittedTimeString[1]);
		}*/
		$('input[data-pointer="' + $(this).parent().parent().attr("id") + '"]')
				.val($(this).val()).change();
		$('select[data-pointer="' + $(this).parent().parent().attr("id") + '"] option:contains(' + $(this)
				.val() + ')')
			.attr('selected', 'selected').change();
	});
	$("#backup-data\\.size").on("change", function() {
		$("#data\\.size\\.min").val($(this).val());
		$("#data\\.size\\.max").val($(this).val());
	});
	//
	$("#backup-load\\.threads").on("change", function() {
		var currentValue = this.value;
		var keys2Override = [
			"#backup-load\\.type\\.append\\.threads",
			"#backup-load\\.type\\.create\\.threads",
			"#backup-load\\.type\\.read\\.threads",
			"#backup-load\\.type\\.update\\.threads",
			"#backup-load\\.type\\.delete\\.threads"
		];
		keys2Override.forEach(function(d) {
			$(d).val(currentValue).change();
		})
	});
	//
	$("#start").click(function(e) {
		e.preventDefault();
		var runId = document.getElementById("backup-run.id");
		runId.value = runId.defaultValue;
		onStartButtonPressed();
	});
	//
	$(".stop").click(function() {
		var currentRunId = $(this).val();
		var currentButton = $(this);
		$.post("/stop", { "run.id" : currentRunId, "type" : "stop" }, function() {
			$("#scenarioTab-" + currentRunId.split(".").join("_") + " .stop").remove();
		}).fail(function() {
			alert("Internal Server Error");
			$("#scenarioTab-" + currentRunId.split(".").join("_") + " .stop").remove();
		});
	});
	//
	$(".kill").click(function() {
		var currentElement = $(this);
		var currentRunId = $(this).attr("value");
		if (confirm("Please note that the test will be shut down if it's running.") === true) {
			$.post("/stop", { "run.id" : currentRunId, "type" : "remove" }, function() {
				$("#" + currentRunId).remove();
				currentElement.parent().remove();
				$('a[href="#configuration"]').tab('show');
			});
		}
	});
	//
	$(".folders a, .folders label").click(function(e) {
		if ($(this).is("a")) {
			e.preventDefault();
		}
		//
		onMenuItemClick($(this));
	});
	//
	$("#chain-load").click(function() {
		$("#backup-chain").modal('show').css("z-index", 5000);
	});
	//
	$("#save-config").click(function() {
		$.post("/save", $("#main-form").serialize(), function() {
			alert("Config was successfully saved");
		});
	});
	//
	$("#save-file").click(function(e) {
		e.preventDefault();
		$.get("/save", $("#main-form").serialize(), function() {
			window.location.href = "/save/config.txt";
		});
	});
	//
	$("#config-file").change(function() {
		var input = $(this).get(0);
		loadPropertiesFromFile(input.files[0]);
	});
	//
	$("#file-checkbox").change(function() {
		if ($(this).is(":checked")) {
			$("#config-file").show();
		} else {
			$("#config-file").hide();
		}
	});
	//
	/*$(".property").click(function() {
		var id = $(this).attr("href").replace(/\./g, "\\.");
		var name = $(this).parents(".file").find(".props").text();
		$("#" + name).show();
		var element = $("#" + name).find($(id));
		var parent = element.parents(".form-group");
		$("#" + name).children().hide();
		parent.show();
	});*/
	//
});

/* Functions */
function excludeDuplicateOptions() {
	var found = [];
	var selectArray = $("select");
	selectArray.each(function() {
		found = [];
		var currentSelect = $(this).children();
		currentSelect.each(function() {
			if ($.inArray(this.value, found) != -1) {
				$(this).remove();
			}
			found.push(this.value);
		});
	});
}
//
function walkTreeMap(map, ul, shortsPropsMap, fullKeyName) {
	$.each(map, function(key, value) {
		var element;
		var currentKeyName = "";
		if (key !== "properties") {
			if (!fullKeyName) {
				currentKeyName = key;
			} else {
				currentKeyName = fullKeyName + "." + key;
			}
		}
		if (!(value instanceof Object)) {
			if (currentKeyName === "run.mode")
				return;
			ul.addChild("<li>")
				.addClass("file")
				.append($("<a>", {
					class: "props",
					href: "#" + currentKeyName,
					text: key
				}));
			shortsPropsMap[currentKeyName] = value;
		} else {
			element = ul.prependChild("<li>")
					.append($("<label>", {
						for: key,
						text: key
					}))
					.append($("<input>", {
						type: "checkbox",
						id: key
					}))
					.addChild("<ul>");
			walkTreeMap(value, element, shortsPropsMap, currentKeyName);
		}
	});
}
//
function buildDivBlocksByPropertyNames(shortPropsMap) {
	for (var key in shortPropsMap) {
		if (shortPropsMap.hasOwnProperty(key)) {
			if (key === "run.mode")
				continue;
			var keyDiv = $("<div>").addClass("form-group");
			keyDiv.attr("id", key);
			keyDiv.css("display", "none");
			var placeHolder = "";
			if (key === "data.src.fpath") {
				placeHolder = "Format: log/<run.mode>/<run.id>/<filename>";
			}
			keyDiv.append($("<label>", {
				for: key,
				class: "col-sm-3 control-label",
				text: key.split(".").pop()
			}))
			.append($("<div>", {
				class: "col-sm-9"
			}).append($("<input>", {
				type: "text",
				class: "form-control",
				name: key,
				value: shortPropsMap[key],
				placeholder: "Enter '" + key + "' property. " + placeHolder
			})));
			keyDiv.appendTo("#configuration-content");
		}
	}
}
//
jQuery.fn.addChild = function(html) {
	var target = $(this[0]);
	var child = $(html);
	child.appendTo(target);
	return child;
};
//
jQuery.fn.prependChild = function(html) {
	var target = $(this[0]);
	var child = $(html);
	child.prependTo(target);
	return child;
};
//
function generatePropertyPage() {
	if (!$("#properties").is(":checked")) {
		$("#properties").trigger("click");
	}
	onMenuItemClick($('a[href="#auth"]'));
}
//
function onMenuItemClick(element) {
	resetParams();
	element.css("color", "#CC0033");
	if (element.is("a")) {
		var id = element.attr("href").replace(/\./g, "\\.");
		var block = $(id);
		block.show();
		block.children().show();
	}
}
//
function resetParams() {
	$("a, label").css("color", "");
	$("#configuration-content").children().hide();
}
//
function configureWebSocketConnection(location, countOfRecords) {
	var RUN_SCENARIO_NAME = {
		single: "single",
		chain: "chain",
		rampup: "rampup"
	};
	var MARKERS = {
		ERR: "err",
		MSG: "msg",
		PERF_SUM: "perfSum",
		PERF_AVG: "perfAvg"
	};
	var LOG_FILES = {
		ERR: "errors-log",
		MSG: "messages-csv",
		PERF_SUM: "perf-sum-csv",
		PERF_AVG: "perf-avg-csv"
	};
	return {
		connect: function(chartsArray) {
			this.ws = new WebSocket(location);
			this.ws.onmessage = function(message) {
				var json = JSON.parse(message.data);
				var runId = json.contextMap["run.id"];
				var runMetricsPeriodSec = json.contextMap["load.metricsPeriodSec"];
				var scenarioChainLoad = json.contextMap["scenario.type.chain.load"];
				var rampupThreadCounts = json.contextMap["scenario.type.rampup.threadCounts"];
				var loadRampupSizes = json.contextMap["scenario.type.rampup.sizes"];
				//
				var entry = runId.split(".").join("_");
				if (!json.hasOwnProperty("marker") || !json.loggerName) {
					return;
				}
				if (json.marker === null)
					return;
				var isContains = false;
				chartsArray.forEach(function(d) {
					if (d["run.id"] === runId) {
						isContains = true;
					}
				});
				if (!isContains) {
					if (json.contextMap["scenario.name"] === RUN_SCENARIO_NAME.rampup) {
						charts(chartsArray).rampup(runId, scenarioChainLoad, rampupThreadCounts, loadRampupSizes);
					}
				}
				switch (json.marker.name) {
					case MARKERS.ERR:
						appendMessageToTable(entry, LOG_FILES.ERR, countOfRecords, json);
						break;
					case MARKERS.MSG:
						appendMessageToTable(entry, LOG_FILES.MSG, countOfRecords, json);
						break;
					case MARKERS.PERF_SUM:
						appendMessageToTable(entry, LOG_FILES.PERF_SUM, countOfRecords, json);
						if (json.contextMap["scenario.name"] === RUN_SCENARIO_NAME.rampup) {
							//alert(json.contextMap["currentSize"]);
							chartsArray.forEach(function(d) {
								//console.log(d);
								if (d["run.id"] === runId) {
									d.charts.forEach(function(c) {
										c.update(json);
									});
								}
							});
						}
						/*if (json.contextMap["run.scenario.name"] === RUN_SCENARIO_NAME.rampup) {
					        charts(chartsArray).rampup(runId, scenarioChainLoad, rampupThreadCounts, loadRampupSizes);
					    }*/
						break;
					case MARKERS.PERF_AVG:
						appendMessageToTable(entry, LOG_FILES.PERF_AVG, countOfRecords, json);
						var isFound = false;
						chartsArray.forEach(function(d) {
							if (d["run.id"] === runId) {
								isFound = true;
								d.charts.forEach(function(c) {
									c.update(json);
								});
							}
						});
						if (!isFound) {
							switch(json.contextMap["scenario.name"]) {
								case RUN_SCENARIO_NAME.single:
									charts(chartsArray).single(json);
									break;
								case RUN_SCENARIO_NAME.chain:
									if (json.threadName.indexOf("remote") > -1) {
										json.threadName = json.threadName.substring(0, json.threadName.lastIndexOf("-"));
									}
									charts(chartsArray).chain(runId, runMetricsPeriodSec, json.threadName);
									break;
							}
						}
						break;
				}
			};
		}
	};
}
//
function appendMessageToTable(entry, tableName, countOfRows, message) {
	if ($("#" + entry + "-" +tableName + " table tbody tr").length > countOfRows) {
		$("#" + entry + "-" + tableName + " table tbody tr:first-child").remove();
	}
	$("#" + entry + "-" + tableName +" table tbody").append(getTableRowByMessage(message));
}
//
function getTableRowByMessage(json) {
	return '<tr>\
			<td>' + json.level.standardLevel + '</td>\
			<td>' + json.loggerName + '</td>\
			<td>' + json.threadName + '</td>\
			<td>' + new Date(json.timeMillis) + '</td>\
			<td>' + json.message.formattedMessage + '</td>\
			</tr>';
}
//
function onStartButtonPressed() {
	$.post("/start", $("#main-form").serialize(), function(data) {
		if (data) {
			if (confirm("Are you sure? " + data) === true) {
				$.post("/stop", { "run.id" : $("#run\\.id").val(), "type" : "remove" }, function(data, status) {
					if (status) {
						onStartButtonPressed();
					}
				}).fail(function() {
					alert("Internal Server Error");
				});
			}
		} else {
			$('#config-type option').prop('selected', function() {
				return this.defaultSelected;
			});
			location.reload();
		}
	});
}
//
function loadPropertiesFromFile(file) {
	var reader = new FileReader();
	reader.onload = function() {
		var text = reader.result;
		var lines = text.split("\n");
		for (var i = 0;i < lines.length; i++) {
			var splitLine = lines[i].split(" = ");
			var elementId = "#" + splitLine[0].replace(/\./g, "\\.");
			if ($(elementId).is("input:text")) {
				$(elementId).val(splitLine[1]);
				if ($(elementId).attr("id") === "run.time") {
					var resultArray = $(elementId).val().split(".");
					$(".complex input").val(resultArray[0]);
					$(".complex select option:contains(" + resultArray[1] + ")").attr("selected", "selected");
				} else {
					var input = $('input[data-pointer="' + $(elementId).attr("id") + '"]').val($(elementId).val());
					var select = $('select[data-pointer="' + $(elementId).attr("id") + '"] option:contains(' + $(elementId).val() + ')')
						.attr('selected', 'selected');
					input.change();
					select.change();
				}
			}
		}
	};
	reader.readAsText(file);
}
//
//  Charts
function charts(chartsArray) {
	var margin = {top: 40, right: 200, bottom: 60, left: 60},
		width = 1070 - margin.left - margin.right,
		height = 460 - margin.top - margin.bottom;
	//  Some constants
	var SCENARIO = {
		single: "single",
		chain: "chain",
		rampup: "rampup"
	};
	var CHART_TYPES = {
		TP: "throughput",
		BW: "bandwidth"
	};
	var AVG = "total average",
		MIN_1 = "last 1 min avg",
		MIN_5 = "last 5 min avg",
		MIN_15 = "last 15 min avg";
	//  Some constants from runTimeConfig
	var RUN_TIME_CONFIG_CONSTANTS = {
		runId: "run.id",
		runMetricsPeriodSec: "load.metricsPeriodSec",
		runScenarioName: "scenario.name"
	};
	//
	var colorsList18 = [
		"#0000CD",
		"#006400",
		"#8B0000",
		"#8B008B",
		"#008B8B",
		"#808000",
		"#FF8C00",
		"#556B2F",
		"#8A2BE2",
		"#00FA9A",
		"#C71585",
		"#8B4513",
		"#00CED1",
		"#191970",
		"#2F4F4F",
		"#FF00FF",
		"#BDB76B",
		"#BC8F8F"
	];
	var CHART_MODES = [AVG, MIN_1, MIN_5, MIN_15];
	//  Common functions for charts
	function getScenarioChartObject(runId, runScenarioName, scenarioCharts) {
		return {
			"run.id": runId,
			"run.scenario.name": runScenarioName,
			"charts": scenarioCharts
		};
	}
	//
	function drawThroughputCharts(data, json) {
		var updateFunction = drawChart(data, json, "seconds[s]", "TP[obj/s]",
				"#tp-" + json.contextMap[RUN_TIME_CONFIG_CONSTANTS.runId].split(".").join("_"));
		return {
			update: function(json) {
				updateFunction(CHART_TYPES.TP, json.message.formattedMessage);
			}
		};
	}
	//
	function drawBandwidthCharts(data, json) {
		var updateFunction = drawChart(data, json, "seconds[s]", "BW[MB/s]",
				"#bw-" + json.contextMap[RUN_TIME_CONFIG_CONSTANTS.runId].split(".").join("_"));
		return {
			update: function(json) {
				updateFunction(CHART_TYPES.BW, json.message.formattedMessage);
			}
		};
	}
	//
	function drawChart(data, json, xAxisLabel, yAxisLabel, chartDOMPath) {
		//  get some fields from runTimeConfig
		var runMetricsPeriodSec = json.contextMap[RUN_TIME_CONFIG_CONSTANTS.runMetricsPeriodSec];
		//var runScenarioName = json.contextMap[RUN_TIME_CONFIG_CONSTANTS.runScenarioName];
		//
		var x = d3.scale.linear()
			.domain([
				d3.min(data, function(c) { return d3.min(c.values, function(d) { return d.x; }); }),
				d3.max(data, function(c) { return d3.max(c.values, function(d) { return d.x; })  })
			])
			.range([0, width]);

		var y = d3.scale.linear()
			.domain([
				d3.min(data, function(c) { return d3.min(c.values, function(d) { return d.y; }); }),
				d3.max(data, function(c) { return d3.max(c.values, function(d) { return d.y; })  })
			])
			.range([height, 0]);
		//
		var color = d3.scale.ordinal().
			range(colorsList18);
		color.domain(data.map(function(d) { return d.name; }));
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
				return x(d.x);
			})
			.y(function (d) {
				return y(d.y);
			});
		//
		var svg = d3.select(chartDOMPath)
			.append("svg")
			.attr("width", width + margin.left + margin.right)
			.attr("height", height + margin.top + margin.bottom)
			.append("g")
			.attr("transform", "translate(" + margin.left + "," + margin.top + ")");

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

		var levels = svg.selectAll(".level")
			.data(data).enter()
			.append("g")
			.attr("class", "level")
			.attr("id", function(d, i) { return chartDOMPath.replace("#", "") + d.name; })
			.attr("visibility", function(d) { if (d.name === AVG) { return "visible"; } else { return "hidden"; }})
			.append("path")
			.attr("class", "line")
			.attr("d", function(d)  { return line(d.values); })
			.attr("stroke", function(d) { return color(d.name); });
		//  Axis X Label
		svg.append("text")
			.attr("x", width - 2)
			.attr("y", height - 2)
			.style("text-anchor", "end")
			.text(xAxisLabel);

		//  Axis Y Label
		svg.append("text")
			.attr("transform", "rotate(-90)")
			.attr("y", 6)
			.attr("x", 0)
			.attr("dy", ".71em")
			.style("text-anchor", "end")
			.text(yAxisLabel);
		//
		svg.selectAll("foreignObject")
			.data(data).enter()
			.append("foreignObject")
			.attr("x", width + 3)
			.attr("width", 18)
			.attr("height", 18)
			.attr("transform", function(d, i) {
				return "translate(0," + i * 20 + ")";
			})
			.append("xhtml:body")
			.append("input")
			.attr("type", "checkbox")
			.attr("value", function(d) { return d.name; })
			.attr("checked", function(d) { if (d.name === AVG) { return "checked"; } })
			.on("click", function(d, i) {
				var element = $(chartDOMPath + d.name);
				if ($(this).is(":checked")) {
					element.css("visibility", "visible")
				} else {
					element.css("visibility", "hidden");
				}
			});
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
			.style("fill", function(d) { return color(d.name); });

		legend.append("text")
			.attr("x", width + 40)
			.attr("y", 9)
			.attr("dy", ".35em")
			.style("text-anchor", "start")
			.text(function(d) { return d.name; });

		svg.append("text")
			.attr("x", (width / 2))
			.attr("y", 0 - (margin.top / 2))
			.attr("text-anchor", "middle")
			.style("font-size", "16px")
			.style("text-decoration", "underline")
			.text(json.threadName);
		return function(chartType, value) {
			var splitIndex = 0;
			switch(chartType) {
				case CHART_TYPES.TP:
					splitIndex = 2;
					break;
				case CHART_TYPES.BW:
					splitIndex = 3;
					break;
			}
			//
			var parsedString = value.split(";")[splitIndex];
			var first = parsedString.indexOf("(") + 1;
			var second = parsedString.lastIndexOf(")");
			value = parsedString.substring(first, second).split("/");
			//
			data.forEach(function(d, i) {
				d.values.push({x: d.values.length * runMetricsPeriodSec, y: parseFloat(value[i])});
			});
			//
			x.domain([
				d3.min(data, function(c) { return d3.min(c.values, function(d) { return d.x; }); }),
				d3.max(data, function(c) { return d3.max(c.values, function(d) { return d.x; }); })
			]);
			y.domain([
				d3.min(data, function(c) { return d3.min(c.values, function(d) { return d.y; }); }),
				d3.max(data, function(c) { return d3.max(c.values, function(d) { return d.y; }); })
			]);
			//
			xAxisGroup.transition().call(xAxis);
			yAxisGroup.transition().call(yAxis);
			xGrid.call(makeXAxis()
				.tickSize(-height, 0, 0)
				.tickFormat(""));
			yGrid.call(makeYAxis()
				.tickSize(-width, 0, 0)
				.tickFormat(""));
			//  Update old charts
			var paths = svg.selectAll(".level path")
				.data(data)
				.attr("d", function(d) { return line(d.values); })
				.attr("stroke", function(d) { return color(d.name); })
				.attr("fill", "none");
		};
	}
	//
	return {
		single: function(json) {
			//  get some fields from runTimeConfig
			var runId = json.contextMap[RUN_TIME_CONFIG_CONSTANTS.runId];
			var runScenarioName = json.contextMap[RUN_TIME_CONFIG_CONSTANTS.runScenarioName];
			//
			var data = [
				{
					name: AVG,
					values: [
						{x: 0, y: 0}
					]
				}, {
					name: MIN_1,
					values: [
						{x: 0, y: 0}
					]
				}, {
					name: MIN_5,
					values: [
						{x: 0, y: 0}
					]
				}, {
					name: MIN_15,
					values: [
						{x: 0, y: 0}
					]
				}
			];
			chartsArray.push(getScenarioChartObject(runId, runScenarioName,
					[drawThroughputCharts($.extend(true, [], data), json),
						drawBandwidthCharts($.extend(true, [], data), json)]));
		},
		chain: function(runId, runMetricsPeriodSec, loadType) {
			var AVG = "total average",
				MIN_1 = "last 1 min avg",
				MIN_5 = "last 5 min avg",
				MIN_15 = "last 15 min avg";
			//
			var TP_MODES = [AVG, MIN_1, MIN_5, MIN_15];
			//
			var CHART_TYPES = {
				TP: "throughput",
				BW: "bandwidth"
			};
			//
			chartsArray.push({
				"run.id": runId,
				"run.scenario.name": SCENARIO.chain,
				"charts": [
					drawThroughputChart(),
					drawBandwidthChart()
				]
			});
			//
			function drawThroughputChart() {
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
								name: MIN_1,
								values: [
									{x: 0, y: 0}
								]
							}, {
								name: MIN_5,
								values: [
									{x: 0, y: 0}
								]
							}, {
								name: MIN_15,
								values: [
									{x: 0, y: 0}
								]
							}
						]
					}
				];
				var updateFunction = drawChart(data, "Throughput[obj/s]", "seconds", "throughput[obj/s]", "#tp-" + runId.split(".").join("_"));
				return {
					update: function(json) {
						updateFunction(CHART_TYPES.TP, json);
					}
				};
			}
			//
			function drawBandwidthChart() {
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
								name: MIN_1,
								values: [
									{x: 0, y: 0}
								]
							}, {
								name: MIN_5,
								values: [
									{x: 0, y: 0}
								]
							}, {
								name: MIN_15,
								values: [
									{x: 0, y: 0}
								]
							}
						]
					}
				];
				var updateFunction = drawChart(data, "Bandwidth[MB/s]", "seconds", "bandwidth[obj/s]", "#bw-" + runId.split(".").join("_"));
				return {
					update: function(json) {
						updateFunction(CHART_TYPES.BW, json);
					}
				};
			}
			//
			function drawChart(data, chartTitle, xAxisLabel, yAxisLabel, path) {
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

				var y = d3.scale.linear()
					.domain([
						d3.min(data, function(d) { return d3.min(d.charts, function(c) {
							return d3.min(c.values, function(v) { return v.y; }); });
						}),
						d3.max(data, function(d) { return d3.max(d.charts, function(c) {
							return d3.max(c.values, function(v) { return v.y; }); });
						})
					])
					.range([height, 0]);
				//
				var color = d3.scale.ordinal().range(colorsList18);
				color.domain(data.map(function(d) { return d.loadType; }));
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
						return x(d.x);
					})
					.y(function (d) {
						return y(d.y);
					});
				//
				var svg = d3.select(path)
					.append("svg")
					.attr("width", width + margin.left + margin.right)
					.attr("height", height + margin.top + margin.bottom)
					.append("g")
					.attr("transform", "translate(" + margin.left + "," + margin.top + ")");

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
					.attr("class", "line")
					.attr("d", function(c) { return line(c.values); })
					.attr("stroke-dasharray", function(c, i) {
						switch (c.name) {
							case AVG:
								return "0,0";
								break;
							case MIN_1:
								return "3,3";
								break;
							case MIN_5:
								return "10,10";
								break;
							case MIN_15:
								return "20,10,5,5,5,10";
								break;
						}
					})
					.attr("id", function(c) {
						return path.replace("#", "") + loadType + "-" + c.name;
					})
					.attr("visibility", function(c) { if (c.name === AVG) { return "visible"; } else { return "hidden"; }});
				//
				svg.selectAll(".right-foreign")
					.data(data).enter()
					.append("foreignObject")
					.attr("class", "right-foreign")
					.attr("x", width + 3)
					.attr("width", 18)
					.attr("height", 18)
					.attr("transform", function(d, i) {
						return "translate(0," + i * 20 + ")";
					})
					.append("xhtml:body")
					.append("input")
					.attr("type", "checkbox")
					.attr("value", function(d) { return d.name; })
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
				svg.selectAll(".bottom-foreign")
					.data(TP_MODES).enter()
					.append("foreignObject")
					.attr("class", "bottom-foreign")
					.attr("width", 18)
					.attr("height", 18)
					.attr("transform", function(d, i) {
						return "translate(" + (i*210 + 20) + "," + (height + (margin.bottom/2) + 4) + ")";
					})
					.append("xhtml:body")
					.append("input")
					.attr("type", "checkbox")
					.attr("class", "bottom-checkbox")
					.attr("value", function(d) { return d; })
					.attr("checked", function(d) { if (d === AVG) { return "checked"; } })
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
					.data(TP_MODES).enter()
					.append("g")
					.attr("class", "bottom-legend")
					.attr("stroke", "black")
					.attr("transform", function(d, i) {
						return "translate(" + i*210 + "," + (height + (margin.bottom/2)) + ")";
					});

				bottomLegend.append("path")
					.attr("d", function(d, i) {
						switch(d) {
						case AVG:
							return "M20 0 L110 0";
							break;
						case MIN_1:
							return "M20 0 L115 0";
							break;
						case MIN_5:
						case MIN_15:
							return "M20 0 L120 0";
							break;
						}
					})
					.attr("stroke-dasharray", function(d, i) {
						switch (d) {
							case AVG:
								return "0,0";
								break;
							case MIN_1:
								return "3,3";
								break;
							case MIN_5:
								return "10,10";
								break;
							case MIN_15:
								return "20,10,5,5,5,10";
								break;
						}
					});
				bottomLegend.append("text")
					.attr("x", 35)
					.attr("y", 15)
					.attr("dy", ".35em")
					.style("text-anchor", "start")
					.attr("stroke", "none")
					.attr("stroke-width", "none")
					.text(function(d) { return d; });
				//  Axis X Label
				svg.append("text")
					.attr("x", width - 2)
					.attr("y", height - 2)
					.style("text-anchor", "end")
					.text(xAxisLabel);

				//  Axis Y Label
				svg.append("text")
					.attr("transform", "rotate(-90)")
					.attr("y", 6)
					.attr("x", 0)
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
				return function(chartType, json) {
					if (json.threadName.indexOf("remote") > -1) {
						json.threadName = json.threadName.substring(0, json.threadName.lastIndexOf("-"));
					}
					var loadType = json.threadName;
					//
					var splitIndex = 0;
					switch(chartType) {
						case CHART_TYPES.TP:
							splitIndex = 2;
							break;
						case CHART_TYPES.BW:
							splitIndex = 3;
							break;
					}
					//
					var parsedString = json.message.formattedMessage.split(";")[splitIndex];
					var first = parsedString.indexOf("(") + 1;
					var second = parsedString.lastIndexOf(")");
					var value = parsedString.substring(first, second).split("/");
					//
					var isFound = false;
					data.forEach(function(d) {
						if (d.loadType === loadType) {
							isFound = true;
							d.charts.forEach(function(c, i) {
								c.values.push({x: c.values.length * runMetricsPeriodSec, y: parseFloat(value[i])});
							})
						}
					});
					if (!isFound) {
						var d = {
							loadType: loadType,
							charts: [
								{
									name: AVG,
									values: [
										{x: 0, y: 0}
									]
								}, {
									name: MIN_1,
									values: [
										{x: 0, y: 0}
									]
								}, {
									name: MIN_5,
									values: [
										{x: 0, y: 0}
									]
								}, {
									name: MIN_15,
									values: [
										{x: 0, y: 0}
									]
								}
							]
						};
						/*d.charts.forEach(function(c, i) {
							c.values.push({x: c.values.length * 10, y: parseFloat(value[i])})
						});*/
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
								if (i === 3) {
									return "20,10,5,5,5,10";
								}
								return i*15 + "," + i*15;
							})
							.attr("id", function(c) { return path.replace("#", "") + loadType + "-" + c.name; })
							.attr("visibility", function(c) {
								var elements = $(path + " " + ".bottom-checkbox:checked");
								var isFound = false;
								elements.each(function() {
									if (c.name === $(this).val()) {
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
					x.domain([
						d3.min(data, function(d) { return d3.min(d.charts, function(c) {
							return d3.min(c.values, function(v) { return v.x; }); });
						}),
						d3.max(data, function(d) { return d3.max(d.charts, function(c) {
							return d3.max(c.values, function(v) { return v.x; }); });
						})
					]);
					y.domain([
						d3.min(data, function(d) { return d3.min(d.charts, function(c) {
							return d3.min(c.values, function(v) { return v.y; }); });
						}),
						d3.max(data, function(d) { return d3.max(d.charts, function(c) {
							return d3.max(c.values, function(v) { return v.y; }); });
						})
					]);
					//
					xAxisGroup.transition().call(xAxis);
					yAxisGroup.transition().call(yAxis);
					xGrid.call(makeXAxis()
						.tickSize(-height, 0, 0)
						.tickFormat(""));
					yGrid.call(makeYAxis()
						.tickSize(-width, 0, 0)
						.tickFormat(""));
					//  Update old charts
					/*var paths = svg.selectAll(".level path")
						.data(data)
						.attr("d", function(d) { return line(d.values); })
						.attr("stroke", function(d) { return color(d.name); })
						.attr("fill", "none");*/

					var paths = svg.selectAll(".level")
						.data(data)
						.selectAll("path")
						.data(function(d) {
							return d.charts;
						})
						.attr("d", function(c) { return line(c.values); })
						.attr("stroke-dasharray", function(c, i) {
							switch (c.name) {
								case AVG:
									return "0,0";
									break;
								case MIN_1:
									return "3,3";
									break;
								case MIN_5:
									return "10,10";
									break;
								case MIN_15:
									return "20,10,5,5,5,10";
									break;
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
						.attr("class", "right-foreign")
						.attr("x", width + 3)
						.attr("width", 18)
						.attr("height", 18)
						.attr("transform", function(d, i) {
							return "translate(0," + i * 20 + ")";
						})
						.append("xhtml:body")
						.append("input")
						.attr("type", "checkbox")
						.attr("value", function(d) { return d.name; })
						.attr("checked", "checked")
						.on("click", function(d, i) {
							var element = $(path + d.loadType);
							if ($(this).is(":checked")) {
								element.css("opacity", "1")
							} else {
								element.css("opacity", "0");
							}
						});
				};
			}
		},
		rampup: function(runId, scenarioChainLoad, rampupThreadCounts, loadRampupSizes) {
			//
			// change default width
			width = 480;
			//
			var loadTypes = scenarioChainLoad.split(",");
			var rampupThreadCountsArray = rampupThreadCounts.split(",").map(function(item) {
				return parseInt(item, 10);
			});
			var loadRampupSizesArray = loadRampupSizes.split(",").map(function(item) {
				return item.trim();
			});
			var AVG = "total average",
				MIN_1 = "last 1 min avg",
				MIN_5 = "last 5 min avg",
				MIN_15 = "last 15 min avg";
			//
			var CHART_TYPES = {
				TP: "throughput",
				BW: "bandwidth"
			};
			//
			var TP_MODES = [AVG, MIN_1, MIN_5, MIN_15];
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
				var updateFunction = drawCharts(data, "thread count", "throughput[obj/s]", "#tp-" + runId.split(".").join("_"));
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
				var updateFunction = drawCharts(data, "thread count", "bandwidth[MB/s]", "#bw-" + runId.split(".").join("_"));
				return {
					update: function(json) {
						updateFunction(CHART_TYPES.BW, json);
					}
				};
			}

			function drawCharts(data, xAxisLabel, yAxisLabel, path) {
				data.forEach(function(d) {
					var x = d3.scale.ordinal()
						.domain(rampupThreadCountsArray)
						.rangePoints([0, width]);
					var y = d3.scale.linear()
						.domain([
							d3.min(d.sizes, function(c) { return d3.min(c.charts, function(v) {
								return d3.min(v.values, function(val) { return val.y; });
							}); }),
							d3.max(d.sizes, function(c) { return d3.max(c.charts, function(v) {
								return d3.max(v.values, function(val) { return val.y; });
							}); })
						])
						.range([height, 0]);
					var color = d3.scale.ordinal()
						.range(colorsList18);
					color.domain(loadRampupSizesArray.map(function(d) { return d; }));
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
						.x(function(d) {
							return x(d.x);
						})
						.y(function(d) {
							return y(d.y);
						});
					var svg = d3.select(path).append("svg")
						.attr("width", width + margin.left + margin.right)
						.attr("height", height + margin.top + margin.bottom)
						.attr("id", path.replace("#", "") + "-" + d.loadType)
						.append("g")
						.attr("transform", "translate(" + margin.left + "," + margin.top + ")");
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
					//  Axis Y Label
					svg.append("text")
						.attr("x", width - 2)
						.attr("y", height - 2)
						.style("text-anchor", "end")
						.text(xAxisLabel);
					//  Axis Y Label
					svg.append("text")
						.attr("transform", "rotate(-90)")
						.attr("y", 6)
						.attr("x", 0)
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
						.attr("class", "right-foreign")
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
				});
				//
				return function(chartType, json) {
					var isFound = false;
					data.forEach(function(d, i) {
						if (json.message.formattedMessage.split(" ")[0].slice(1, -1).toLowerCase().indexOf(d.loadType) > -1) {
							var loadTypeSvg = d3.select(path + "-" + d.loadType);
							//
							var currentLoadType = d.loadType;
							var currentSizes = d.sizes;
							var splitIndex;
							switch(chartType) {
								case CHART_TYPES.TP:
									splitIndex = 2;
									break;
								case CHART_TYPES.BW:
									splitIndex = 3;
									break;
							}
							//
							var parsedString = json.message.formattedMessage.split(";")[splitIndex];
							var first = parsedString.indexOf("(") + 1;
							var second = parsedString.lastIndexOf(")");
							var value = parsedString.substring(first, second).split("/");

							var x = d3.scale.ordinal()
								.domain(rampupThreadCountsArray)
								.rangePoints([0, width]);

							d.sizes.forEach(function(d, i) {
								if (d.size === json.contextMap["currentSize"]) {
									d.charts.forEach(function(c, i) {
										c.values.push({ x: parseInt(json.contextMap["currentThreadCount"]), y: parseFloat(value[i]) });
									});
									//
									var y = d3.scale.linear()
										.domain([
											0,
											d3.max(currentSizes, function(c) { return d3.max(c.charts, function(v) {
												return d3.max(v.values, function(val) { return val.y; });
											}); })
										]).range([height, 0]);
									var line = d3.svg.line()
										.x(function(d) {
											return x(d.x);
										})
										.y(function(d) {
											return y(d.y);
										});
									function makeYAxis() {
										return d3.svg.axis()
											.scale(y)
											.orient("left");
									}
									var yAxis = d3.svg.axis()
										.scale(y)
										.orient("left");
									var yAxisGroup = loadTypeSvg.select(".y-axis")
										.call(yAxis);

									var yGrid = loadTypeSvg.select(".y-grid")
										.call(makeYAxis()
											.tickSize(-width, 0, 0)
											.tickFormat(""));

									yAxisGroup.transition().call(yAxis);
									yGrid.call(makeYAxis()
										.tickSize(-width, 0, 0)
										.tickFormat(""));
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
										.attr("cx", function(coord) { return x(coord.x); })
										.attr("cy", function(coord) { return y(coord.y); })
										.attr("r", 2);
									//  Update dots
									loadTypeSvg.select(path + "-" + currentLoadType + "-" + d.size + "-" + i)
										.selectAll(".dot").data(function(v) { return v.values; })
										.attr("cx", function(coord) { return x(coord.x); })
										.attr("cy", function(coord) { return y(coord.y); });
								}
							});
						}
					});
				}
			}
		}
	}
}
