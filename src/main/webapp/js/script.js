$(document).ready(function() {
	var WEBSOCKET_URL = "ws://" + window.location.host + "/logs";
	var TABLE_ROWS_COUNT = 2050;
	excludeDuplicateOptions();
	//
	var chartsArray = [];
	var shortPropsMap = {};
	var ul = $(".folders");
	//
	walkTreeMap(propertiesMap, ul, shortPropsMap);
	buildDivBlocksByFileNames(shortPropsMap);
	generatePropertyPage();
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
	$("#backup-run\\.scenario\\.name").on("change", function() {
		var valueSelected = this.value;
		$("#scenario-button").attr("data-target", "#" + valueSelected);
	});
	//
	$("#backup-storage\\.api").on("change", function() {
		var valueSelected = this.value;
		$("#api-button").attr("data-target", "#" + valueSelected);
	});
	//
	$("#base input, #base select").on("change", function() {
		var currElement = $(this);
		if (currElement.parents(".complex").length === 1) {
			var input = $("#backup-run\\.time\\.input").val();
			var select = $("#backup-run\\.time\\.select").val();
			currElement = $("#backup-run\\.time").val(input + "." + select);
		}
		//
		var element = document.getElementById(currElement.attr("data-pointer"));
		if (currElement.is("select")) {
			var valueSelected = currElement.children("option").filter(":selected").text().trim();
			$('select[data-pointer="'+currElement.attr("data-pointer")+'"]').val(currElement.val());
			if (element) {
				element.value = valueSelected;
			}
		} else {
			$('input[data-pointer="' + currElement.attr("data-pointer") + '"]').val(currElement.val());
			if (element) {
				element.value = currElement.val();
			}
		}
	});
	//
	$("#extended input").on("change", function() {
		if ($(this).attr("id") === "run.time") {
			var splittedTimeString = $(this).val().split(".");
			$("#backup-run\\.time\\.input").val(splittedTimeString[0]);
			$("#backup-run\\.time\\.select").val(splittedTimeString[1]);
		}
		$('input[data-pointer="' + $(this).attr("id") + '"]').val($(this).val());
		$('select[data-pointer="' + $(this).attr("id") + '"] option:contains(' + $(this).val() + ')')
			.attr('selected', 'selected');
	});
	$("#data-size").on("change", function() {
		$("#data\\.size\\.min").val($(this).val());
		$("#data\\.size\\.max").val($(this).val());
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
			currentButton.remove();
		}).fail(function() {
			alert("Internal Server Error");
			currentButton.remove();
		});
	});
	//
	$(".kill").click(function() {
		var currentElement = $(this);
		var currentRunId = $(this).attr("value");
		if (confirm("Are you sure?") === true) {
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
	$(".property").click(function() {
		var id = $(this).attr("href").replace(/\./g, "\\.");
		var name = $(this).parents(".file").find(".props").text();
		$("#" + name).show();
		var element = $("#" + name).find($(id));
		var parent = element.parents(".form-group");
		$("#" + name).children().hide();
		parent.show();
	});
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
function walkTreeMap(map, ul, shortsPropsMap) {
	$.each(map, function(key, value) {
		var element;
		if (jQuery.isArray(value)) {
			element = ul.addChild("<li>")
				.addClass("file")
				.append($("<a>", {
					class: "props",
					href: "#" + key,
					text: key
				}))
				.append($("<input>", {
					type: "checkbox"
				}))
				.addChild($("<ul>"));
			var array = value;
			for (var i = 0;i < array.length; i++) {
				if (array[i].key === "run.mode") {
					continue;
				}
				element.addChild($("<li>"))
					.addChild($("<a>", {
						href: "#" + array[i].key,
						class: "property",
						text: array[i].value.key
					}));
			}
			shortsPropsMap[key] = value;
			return;
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

		}
		walkTreeMap(value, element, shortsPropsMap);
	});
}
//
function buildDivBlocksByFileNames(shortPropsMap) {
	var formGroupDiv;
	for (var key in shortPropsMap) {
		if (shortPropsMap.hasOwnProperty(key)) {
			var keyDiv = $("<div>").attr("id", key);
			keyDiv.css("display", "none");
			var obj = shortPropsMap[key];
			for (var i = 0; i < obj.length; i++) {
				if (obj[i].key === "run.mode")
					continue;
				formGroupDiv = $("<div>").addClass("form-group");
				var placeHolder = "";
				if (obj[i].key === "data.src.fpath") {
					placeHolder = "Format: log/<run.mode>/<run.id>/<filename>";
				}
				formGroupDiv.append($("<label>", {
					for: obj[i].key,
					class: "col-sm-3 control-label",
					text: obj[i].value.key
					}))
					.append($("<div>", {
						class: "col-sm-9"
					}).append($("<input>", {
						type: "text",
						class: "form-control",
						name: obj[i].key,
						id: obj[i].key,
						value: obj[i].value.value,
						placeholder: "Enter '" + obj[i].key + "' property. " + placeHolder
					})));
				keyDiv.append(formGroupDiv);
			}
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
		var block = $(element.attr("href"));
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
				var runMetricsPeriodSec = json.contextMap["run.metrics.period.sec"];
				var scenarioChainLoad = json.contextMap["scenario.chain.load"];
				var rampupThreadCounts = json.contextMap["scenario.rampup.thread.counts"];
				var loadRampupSizes = json.contextMap["scenario.rampup.sizes"];
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
					if (json.contextMap["run.scenario.name"] === RUN_SCENARIO_NAME.rampup) {
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
						if (json.contextMap["run.scenario.name"] === RUN_SCENARIO_NAME.rampup) {
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
							switch(json.contextMap["run.scenario.name"]) {
								case RUN_SCENARIO_NAME.single:
									charts(chartsArray).single(runId, runMetricsPeriodSec);
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
					if ($(elementId).attr("id") !== "data.count") {
						input.change();
						select.change();
					}
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
		height = 500 - margin.top - margin.bottom;

	var SCENARIO = {
		single: "single",
		chain: "chain",
		rampup: "rampup"
	};

	return {
		single: function(runId, runMetricsPeriodSec, chartTitle) {
			//
			var AVG = "avg";
			var MIN_1 = "1min";
			var MIN_5 = "5min";
			var MIN_15 = "15min";
			//
			var CHART_TYPES = {
				TP: "throughput",
				BW: "bandwidth"
			};
			//
			chartsArray.push({
				"run.id": runId,
				"run.scenario.name": SCENARIO.single,
				"charts": [
					drawThroughputChart(),
					drawBandwidthChart()
				]
			});
			//
			function drawThroughputChart() {
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
				var updateFunction = drawChart(data, "Throughput[obj/s]", "seconds", "throughput[obj/s]", "#tp-" + runId.split(".").join("_"));
				return {
					update: function(json) {
						updateFunction(CHART_TYPES.TP, json.message.formattedMessage);
					}
				};
			}
			//
			function drawBandwidthChart() {
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
				var updateFunction = drawChart(data, "Bandwidth[MB/s]", "seconds", "bandwidth[mb/s]", "#bw-" + runId.split(".").join("_"));
				return {
					update: function(json) {
						updateFunction(CHART_TYPES.BW, json.message.formattedMessage);
					}
				}

			}
			//
			function drawChart(data, chartTitle, xAxisLabel, yAxisLabel, path) {
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
				var color = d3.scale.category10();
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

				var levels = svg.selectAll(".level")
					.data(data).enter()
					.append("g")
					.attr("class", "level")
					.attr("id", function(d, i) { return path.replace("#", "") + d.name; })
					.attr("visibility", function(d) { if (d.name === MIN_1) { return "visible"; } else { return "hidden"; }})
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
					.attr("checked", function(d) { if (d.name === MIN_1) { return "checked"; } })
					.on("click", function(d, i) {
						var element = $(path + d.name);
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
					.attr("x", width + 80)
					.attr("y", 9)
					.attr("dy", ".35em")
					.style("text-anchor", "end")
					.text(function(d) { return d.name; });

				svg.append("text")
					.attr("x", (width / 2))
					.attr("y", 0 - (margin.top / 2))
					.attr("text-anchor", "middle")
					.style("font-size", "16px")
					.style("text-decoration", "underline")
					.text(chartTitle);
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
		},
		chain: function(runId, runMetricsPeriodSec, loadType) {
			var AVG = "avg";
			var MIN_1 = "1min";
			var MIN_5 = "5min";
			var MIN_15 = "15min";
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
				var color = d3.scale.category10();
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
						if (i === 3) {
							return "20,10,5,5,5,10";
						}
						return i*15 + "," + i*15;
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
					.attr("x", width + 150)
					.attr("y", 9)
					.attr("dy", ".35em")
					.style("text-anchor", "end")
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
					.attr("d", "M0 0 L100 0")
					.attr("stroke-dasharray", function(d, i) {
						if (i === 3) {
							return "20,10,5,5,5,10";
						}
						return i*15 + "," + i*15;
					});
				bottomLegend.append("text")
					.attr("x", 50)
					.attr("y", 15)
					.attr("dy", ".35em")
					.style("text-anchor", "middle")
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
							if (i === 3) {
								return "20,10,5,5,5,10";
							}
							return i*15 + "," + i*15;
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
						.attr("x", width + 150)
						.attr("y", 9)
						.attr("dy", ".35em")
						.style("text-anchor", "end")
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
			var loadTypes = scenarioChainLoad.slice(1, -1).split(",");
			var rampupThreadCountsArray = rampupThreadCounts.slice(1, -1).split(",").map(function(item) {
				return parseInt(item, 10);
			});
			var loadRampupSizesArray = loadRampupSizes.slice(1, -1).split(",").map(function(item) {
				return item.trim();
			});
			console.log(loadRampupSizesArray);
			var AVG = "avg";
			var MIN_1 = "1min";
			var MIN_5 = "5min";
			var MIN_15 = "15min";
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
				var updateFunction = drawCharts(data, "seconds", "throughput[obj/s]", "#tp-" + runId.split(".").join("_"));
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
				var updateFunction = drawCharts(data, "seconds", "bandwidth[MB/s]", "#bw-" + runId.split(".").join("_"));
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
					var color = d3.scale.category10();
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
						.attr("x", width + 80)
						.attr("y", 9)
						.attr("dy", ".35em")
						.style("text-anchor", "end")
						.text(function(d) { return d; });
					//
					var levels = svg.selectAll(".level")
						.data(d.sizes).enter()
						.append("g")
						.attr("class", "level")
						.attr("id", function(c, i) { return path.replace("#", "") + "-" + d.loadType + "-" + c.size + "-" + i; })
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
								}
							});
						}
					});
				}
			}
		}
	}
}
