define([
	"../../charts/main",
	"../../charts/util/common",
	"../../util/constants"
], function(
	chartBase, common, constants
) {
	//
	var webSocketUrl = "ws://" + window.location.host + "/logs";
	var maxTableRowsCount = 100;
	//  some constants
	var scenarioName = constants.getScenarioConstant();
	var markers = {
		ERR: "err",
		MSG: "msg",
		PERF_SUM: "perfSum",
		PERF_AVG: "perfAvg"
	};
	var logFiles = {
		ERR: "errors-log",
		MSG: "messages-csv",
		PERF_SUM: "perf-sum-csv",
		PERF_AVG: "perf-avg-csv"
	};
	//
	var chartsArray = [];
	//
	function start() {
		configureWebSocketConnection(
			webSocketUrl, maxTableRowsCount
		).connect(chartsArray);
	}
	//
	function configureWebSocketConnection(location, countOfRecords) {
		function processJsonLogEvents(chartsArray, json) {
			var runId = json.contextMap["run.id"],
				runMetricsPeriodSec = json.contextMap["load.metricsPeriodSec"],
				scenarioChainLoad = json.contextMap["scenario.type.chain.load"],
				rampupConnCounts = json.contextMap["scenario.type.rampup.connCounts"],
				loadRampupSizes = json.contextMap["scenario.type.rampup.sizes"];

			var entry = runId.split(".").join("_");

			if (!json.hasOwnProperty("marker") || !json.loggerName) {
				return;
			}
			if (json.marker === null)
				return;

			var isContains = false;
			chartsArray.forEach(function(d) {
				if(d["run.id"] == runId) {
					isContains = true;
				}
			});

			if(!isContains) {
				if(json.contextMap["scenario.name"] == scenarioName.rampup) {
					chartBase.charts(chartsArray).rampup(
						runId, scenarioChainLoad, rampupConnCounts, loadRampupSizes
					);
				}
			}

			switch (json.marker.name) {
				case markers.ERR:
					appendMessageToTable(entry, logFiles.ERR, countOfRecords, json);
					break;
				case markers.MSG:
					appendMessageToTable(entry, logFiles.MSG, countOfRecords, json);
					break;
				case markers.PERF_SUM:
					appendMessageToTable(entry, logFiles.PERF_SUM, countOfRecords, json);
					if (json.contextMap["scenario.name"] === scenarioName.rampup) {
						chartsArray.forEach(function(d) {
							if (d["run.id"] === runId) {
								d.charts.forEach(function(c) {
									c.update(json);
								});
							}
						});
					}
					break;
				case markers.PERF_AVG:
					appendMessageToTable(entry, logFiles.PERF_AVG, countOfRecords, json);
					// todo start of handling to change
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
							case scenarioName.single:
								chartBase.charts(chartsArray).single(json);
								break;
							case scenarioName.chain:
								json.threadName = json.threadName.match(common.getThreadNamePattern())[0];
								chartBase.charts(chartsArray).chain(
									runId, runMetricsPeriodSec, json.threadName
								);
								break;
						}
					}
					// todo finish of handling to change
				 break;
			}
		}

		function handleLogEventsArray(chartsArray, runId, logEventsArray) {
			var entry = runId.split(".").join("_");
			var scenario = logEventsArray[0].contextMap["scenario.name"];
			var points = [];
			logEventsArray.forEach(function(element) {
				switch (element.marker.name) {
					case markers.ERR:
						appendMessageToTable(entry, logFiles.ERR, countOfRecords, element);
						break;
					case markers.MSG:
						appendMessageToTable(entry, logFiles.MSG, countOfRecords, element);
						break;
					case markers.PERF_SUM:
						appendMessageToTable(entry, logFiles.PERF_SUM, countOfRecords, element);
						points.push(element);
						break;
					case markers.PERF_AVG:
						appendMessageToTable(entry, logFiles.PERF_AVG, countOfRecords, element);
						points.push(element);
						break;
				}
			});
			if (points.length > 0) {
				var filtered = null;
				switch (scenario) {
					case scenarioName.single:
						filtered = points.filter(function(d) {
							return d.marker.name === markers.PERF_AVG;
						});
						chartBase.charts(chartsArray).single(filtered);
						break;
					case scenarioName.chain:
						filtered = points.filter(function(d) {
							return d.marker.name === markers.PERF_AVG;
						});
						var runMetricsPeriodSec = logEventsArray[0].contextMap["load.metricsPeriodSec"];
						chartBase.charts(chartsArray).chain(runId, runMetricsPeriodSec, null, filtered);
						break;
					case scenarioName.rampup:
						filtered = points.filter(function(d) {
							return d.marker.name === markers.PERF_SUM;
						});
						var scenarioChainLoad = filtered[0].contextMap["scenario.type.chain.load"];
						var rampupConnCounts = filtered[0].contextMap["scenario.type.rampup.connCounts"];
						var loadRampupSizes = filtered[0].contextMap["scenario.type.rampup.sizes"];
						chartBase.charts(chartsArray).rampup(
							runId, scenarioChainLoad, rampupConnCounts, loadRampupSizes
						);
						chartsArray.forEach(function(d) {
							if (d["run.id"] === runId) {
								d.charts.forEach(function(c) {
									filtered.forEach(function(v) {
										c.update(v);
									})
								});
							}
						});
						break;
				}
			}
		}
		return {
			connect: function(chartsArray) {
				this.ws = new WebSocket(location);
				this.ws.onmessage = function(message) {
					var json = JSON.parse(message.data);
					if (json.name == "chrtpckg") {
						//todo write chart handle code here
					} else {
						if ($.isArray(json)) {
							var logEventsByRunId = {};
							json.forEach(function (element) {
								var runId = element.contextMap["run.id"];
								if (element.marker !== null) {
									if (!logEventsByRunId.hasOwnProperty(runId)) {
										logEventsByRunId[runId] = [];
									}
									logEventsByRunId[runId].push(element);
								}
							});
							for (var runId in logEventsByRunId) {
								if (logEventsByRunId.hasOwnProperty(runId)) {
									handleLogEventsArray(chartsArray, runId, logEventsByRunId[runId]);
								}
							}
						} else {
							processJsonLogEvents(chartsArray, json);
						}
					}
				};
			}
		};
	}

	function appendMessageToTable(entry, tableName, countOfRows, message) {
		if ($("#" + entry + "-" +tableName + " table tbody tr").length > countOfRows) {
			$("#" + entry + "-" + tableName + " table tbody tr:first-child").remove();
		}
		$("#" + entry + "-" + tableName +" table tbody").append(getTableRowByMessage(message));
	}

	function getTableRowByMessage(json) {
		return '<tr>\
			<td>' + json.level.standardLevel + '</td>\
			<td>' + json.loggerName + '</td>\
			<td>' + json.threadName + '</td>\
			<td>' + new Date(json.timeMillis) + '</td>\
			<td>' + json.message.formattedMessage + '</td>\
			</tr>';
	}

	return {
		start: start
	};
});