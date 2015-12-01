define([
	"../../charts/main",
	"../../charts/util/common"
], function(
	chartBase, common
) {
	//
	var WEBSOCKET_URL = "ws://" + window.location.host + "/logs";
	var TABLE_ROWS_COUNT = 100;
	//  some constants
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
	//
	var chartsArray = [];
	//
	function start() {
		configureWebSocketConnection(
			WEBSOCKET_URL, TABLE_ROWS_COUNT
		).connect(chartsArray);
	}
	//
	function configureWebSocketConnection(location, countOfRecords) {
		function processJsonLogEvents(chartsArray, json) {
			var runId = json.contextMap["run.id"];
			var runMetricsPeriodSec = json.contextMap["load.metricsPeriodSec"];
			var scenarioChainLoad = json.contextMap["scenario.type.chain.load"];
			var rampupConnCounts = json.contextMap["scenario.type.rampup.connCounts"];
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
				if(d["run.id"] == runId) {
					isContains = true;
				}
			});
			if(!isContains) {
				/*if(json.contextMap["scenario.name"] == RUN_SCENARIO_NAME.rampup) {
					common.charts(chartsArray).rampup(runId, scenarioChainLoad, rampupConnCounts, loadRampupSizes);
				}*/
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
						chartsArray.forEach(function(d) {
							if (d["run.id"] === runId) {
								d.charts.forEach(function(c) {
									c.update(json);
								});
							}
						});
					}
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
								chartBase.charts(chartsArray).single(json);
								//chartBase.charts(chartsArray).single(json, undefined, chartsArray);
								break;
							case RUN_SCENARIO_NAME.chain:
								json.threadName = json.threadName.match(common.getThreadNamePattern())[0];
								chartBase.charts(chartsArray).chain(runId, runMetricsPeriodSec, json.threadName);
								break;
						}
					}
				 break;
			}
		}
		//
		//
		function handleLogEventsArray(chartsArray, runId, logEventsArray) {
			var entry = runId.split(".").join("_");
			var scenarioName = logEventsArray[0].contextMap["scenario.name"];
			var points = [];
			logEventsArray.forEach(function(element) {
				switch (element.marker.name) {
					case MARKERS.ERR:
						appendMessageToTable(entry, LOG_FILES.ERR, countOfRecords, element);
						break;
					case MARKERS.MSG:
						appendMessageToTable(entry, LOG_FILES.MSG, countOfRecords, element);
						break;
					case MARKERS.PERF_SUM:
						appendMessageToTable(entry, LOG_FILES.PERF_SUM, countOfRecords, element);
						points.push(element);
						break;
					case MARKERS.PERF_AVG:
						appendMessageToTable(entry, LOG_FILES.PERF_AVG, countOfRecords, element);
						points.push(element);
						break;
				}
			});
			if (points.length > 0) {
				var filtered = null;
				switch (scenarioName) {
					case RUN_SCENARIO_NAME.single:
						filtered = points.filter(function(d) {
							return d.marker.name === MARKERS.PERF_AVG;
						});
						chartBase.charts(chartsArray).single(filtered);
						//chartBase.charts(chartsArray).single(filtered[0], filtered, chartsArray);
						break;
					case RUN_SCENARIO_NAME.chain:
						filtered = points.filter(function(d) {
							return d.marker.name === MARKERS.PERF_AVG;
						});
						var runMetricsPeriodSec = logEventsArray[0].contextMap["load.metricsPeriodSec"];
						chartBase.charts(chartsArray).chain(runId, runMetricsPeriodSec, null, filtered);
						break;
					case RUN_SCENARIO_NAME.rampup:
						filtered = points.filter(function(d) {
							return d.marker.name === MARKERS.PERF_SUM;
						});
						var scenarioChainLoad = filtered[0].contextMap["scenario.type.chain.load"];
						var rampupConnCounts = filtered[0].contextMap["scenario.type.rampup.connCounts"];
						var loadRampupSizes = filtered[0].contextMap["scenario.type.rampup.sizes"];
						//charts(chartsArray).rampup(runId, scenarioChainLoad, rampupConnCounts, loadRampupSizes);
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
					if ($.isArray(json)) {
						var logEventsByRunId = {};
						json.forEach(function(element) {
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
				};
			}
		};
	}
	//
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
	return {
		start: start
	};
});