require(["./requirejs/conf"], function() {
	require(["d3js", "bootstrap", "./util/visvalingam"], function(d3, bootstrap, vis) {
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
		(function($) {
			$.strRemove = function(theTarget, theString) {
				return $("<div/>").append(
					$(theTarget, theString).remove().end()
				).html();
			};
		})(jQuery);
		//
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
			$("#time input, #time select").on("change", function() {
				var strValue = $("#backup-load\\.limit\\.time\\.value").val() +
					$("#backup-load\\.limit\\.time\\.unit").val().charAt(0);
				$("#load\\.limit\\.time input").val(strValue);
			});
			/*
			 $("#objects input").on("change", function() {
			 var defaultValue = $("#load\\.limit\\.time\\.value input").get(0).defaultValue;
			 $("#load\\.limit\\.time\\.value input").val(defaultValue);
			 $("#backup-load\\.limit\\.time\\.value").val(defaultValue);
			 //
			 var defaultUnit = $("#load\\.limit\\.time\\.unit input").get(0).defaultValue;
			 $("#load\\.limit\\.time\\.unit input").val(defaultUnit);
			 $("#backup-load\\.limit\\.time\\.unit").val(defaultUnit);
			 });*/
			//
			$("#base input, #base select").on("change", function() {
				var currElement = $(this);
				/*if (currElement.parents(".complex").length === 1) {
				 var input = $("#backup-run\\.time\\.input").val();
				 var select = $("#backup-run\\.time\\.select").val();
				 currElement = $("#backup-run\\.time").val(input + "." + select);
				 }*/
				//
				var currDataPointer = currElement.attr("data-pointer");
				if(currDataPointer != null && currDataPointer.length > 0) {
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
				}
			});
			//
			$("#extended input").on("change", function() {
				/*if ($(this).attr("id") === "run.time") {
				 var splittedTimeString = $(this).val().split(".");
				 $("#backup-run\\.time\\.input").val(splittedTimeString[0]);
				 $("#backup-run\\.time\\.select").val(splittedTimeString[1]);
				 }*/
				var parentIdAttr = $(this).parent().parent().attr("id");
				var patternTime = /^([0-9]*)([smhdSMHD]?)$/;
				var numStr = "0", unitStr = "seconds";
				var timeUnitShortCuts = {
					"s" : "seconds",
					"m" : "minutes",
					"h" : "hours",
					"d" : "days"
				};
				//console.log($(this).val())
				if(parentIdAttr == "load.limit.time") {
					var rawValue = $(this).val();
					if(patternTime.test(rawValue)) {
						var matcher = patternTime.exec(rawValue);
						numStr = matcher[1];
						if(matcher[2] != null && matcher[2].length > 0) {
							unitStr = timeUnitShortCuts[matcher[2].toLowerCase()];
						}
					} else if(rawValue.indexOf('.') > 0) {
						var splitValue = rawValue.split('.')
						numStr = splitValue[0];
						unitStr = splitValue[1];
					}
					// ok, going further
					$("#backup-load\\.limit\\.time\\.value").val(numStr);
					$("#backup-load\\.limit\\.time\\.unit").val(unitStr);
				} else {
					var input = $('input[data-pointer="' + parentIdAttr + '"]')
						.val($(this).val());
					var select = $('select[data-pointer="' + parentIdAttr + '"] option:contains(' + $(this)
						.val() + ')')
						.attr('selected', 'selected');
				}
			});
			$("#backup-data\\.size").on("change", function() {
				$("#data\\.size\\.min input").val($(this).val());
				$("#data\\.size\\.max input").val($(this).val());
				//
				$("#data\\.size").val(this.value);
			});
			//
			$("#backup-load\\.connections").on("change", function() {
				var currentValue = this.value;
				var keys2Override = [
					"#backup-load\\.type\\.append\\.connections",
					"#backup-load\\.type\\.create\\.connections",
					"#backup-load\\.type\\.read\\.connections",
					"#backup-load\\.type\\.update\\.connections",
					"#backup-load\\.type\\.delete\\.connections"
				];
				keys2Override.forEach(function(d) {
					$(d).val(currentValue).change();
				});
				//
				$("#load\\.connections").val(this.value);
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
				currentButton.remove();
				$.post("/stop", { "run.id" : currentRunId, "type" : "stop" }, function() {
				}).fail(function() {
					alert("Internal Server Error");
				});
			});
			//
			$(".kill").click(function() {
				var currentElement = $(this);
				var currentRunId = $(this).attr("value");
				if (confirm("Please note that the test will be shut down if it's running.") === true) {
					$("#wait").show();
					$.post("/stop", { "run.id" : currentRunId, "type" : "remove" }, function() {
						$("#" + currentRunId).remove();
						currentElement.parent().remove();
						$('a[href="#configuration"]').tab('show');
						$("#wait").hide();
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
			$("#backup-run\\.id, #run\\.id").change(function() {
				var startBtn = $("#start");
				var currVal = this.value;
			});
		});

		/* Functions */
		//
		function getThreadNamePattern() {
			return /([\d]+)-([A-Za-z0-9]+)-([CreateRdDlUpAn]+)[\d]*-([\d]*)x([\d]*)x?([\d]*)/gi;
		}
		//
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
					if (d["run.id"] === runId) {
						isContains = true;
					}
				});
				if (!isContains) {
					if (json.contextMap["scenario.name"] === RUN_SCENARIO_NAME.rampup) {
						charts(chartsArray).rampup(runId, scenarioChainLoad, rampupConnCounts, loadRampupSizes);
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
									charts(chartsArray).single(json);
									break;
								case RUN_SCENARIO_NAME.chain:
									json.threadName = json.threadName.match(getThreadNamePattern())[0];
									charts(chartsArray).chain(runId, runMetricsPeriodSec, json.threadName);
									break;
							}
						}
						break;
				}
			}
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
							charts(chartsArray).single(filtered[0], filtered);
							break;
						case RUN_SCENARIO_NAME.chain:
							filtered = points.filter(function(d) {
								return d.marker.name === MARKERS.PERF_AVG;
							});
							var runMetricsPeriodSec = logEventsArray[0].contextMap["load.metricsPeriodSec"];
							charts(chartsArray).chain(runId, runMetricsPeriodSec, null, filtered);
							break;
						case RUN_SCENARIO_NAME.rampup:
							filtered = points.filter(function(d) {
								return d.marker.name === MARKERS.PERF_SUM;
							});
							var scenarioChainLoad = filtered[0].contextMap["scenario.type.chain.load"];
							var rampupConnCounts = filtered[0].contextMap["scenario.type.rampup.connCounts"];
							var loadRampupSizes = filtered[0].contextMap["scenario.type.rampup.sizes"];
							charts(chartsArray).rampup(runId, scenarioChainLoad, rampupConnCounts, loadRampupSizes);
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
						$.post("/stop", { "run.id" : $("#run\\.id input").val(), "type" : "remove" },
							function(data, status) {
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
					"limit": 10,
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
			//
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
			//  Common functions for charts
			function getScenarioChartObject(runId, runScenarioName, scenarioCharts) {
				return {
					"run.id": runId,
					"run.scenario.name": runScenarioName,
					"charts": scenarioCharts
				};
			}
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
				var updateFunction = drawChart(data, json, "t[seconds]", "Rate[s^-1]",
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
				var updateFunction = drawChart(data, json, "t[seconds]", "Rate[MB*s^-1]",
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
						var tpPattern = "[\\s]+TP\\[s\\^\\-1\\]=\\(([\\.\\d]+)/([\\.\\d]+)\\);";
						var tpArray = value.match(tpPattern);
						result = tpArray.slice(1, tpArray.length);
						break;
					case CHART_TYPES.BW:
						var bwPattern = "[\\s]+BW\\[MB\\*s\\^\\-1\\]=\\(([\\.\\d]+)/([\\.\\d]+)\\)";
						var bwArray = value.match(bwPattern);
						result = bwArray.slice(1, bwArray.length);
						break;
				}
				//
				return result;
			}
			//
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
						var updateFunction = drawChart(data, "Throughput[obj/s]", "t[seconds]", "Rate[s^-1]", "#tp-" + runId.split(".").join("_"));
						return {
							update: function(json) {
								updateFunction(CHART_TYPES.TP, json);
							}
						};
					}
					//
					function drawBandwidthChart(data) {
						var updateFunction = drawChart(data, "Bandwidth[MB/s]", "t[seconds]", "Rate[MB*s^-1]", "#bw-" + runId.split(".").join("_"));
						return {
							update: function(json) {
								updateFunction(CHART_TYPES.BW, json);
							}
						};
					}
					//
					function drawChart(data, chartTitle, xAxisLabel, yAxisLabel, path) {
						//
						var currXScale = SCALE_TYPES[0];
						var currYScale = SCALE_TYPES[0];
						//
						var currTimeUnit = TIME_LIMITATIONS.seconds;
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

						while (isTimeLimitReached(x.domain()[x.domain().length - 1], currTimeUnit)) {
							if (currTimeUnit.next === null) {
								return;
							}
							currTimeUnit = TIME_LIMITATIONS[currTimeUnit.next];
							xAxisLabel = currTimeUnit.label;
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
									case AVG.id:
										return "0,0";
										break;
									case LAST.id:
										return "3,3";
										break;
								}
							})
							.attr("id", function(c) {
								return path.replace("#", "") + loadType + "-" + c.name.id;
							})
							.attr("visibility", function(c) { if (c.name.id === AVG.id) { return "visible"; } else { return "hidden"; }});
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
						svg.selectAll(".bottom-foreign")
							.data(TP_MODES).enter()
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
							.attr("checked", function(d) { if (d.id === AVG.id) { return "checked"; } })
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
								return "translate(" + i*210 + "," + (height + (margin.bottom/2) + 20) + ")";
							});

						bottomLegend.append("path")
							.attr("d", function(d, i) {
								switch(d.id) {
									case AVG.id:
										return "M20 0 L110 0";
										break;
									case LAST.id:
										return "M20 0 L115 0";
										break;
								}
							})
							.attr("stroke-dasharray", function(d, i) {
								switch (d.id) {
									case AVG.id:
										return "0,0";
										break;
									case LAST.id:
										return "3,3";
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
										case AVG.id:
											return "0,0";
											break;
										case LAST.id:
											return "3,3";
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
						appendScaleLabels(svg, {
							updateScales: function(scaleOrientation, scaleType) {
								// find scale and apply params to it
								if (scaleType === SCALE_TYPES[0]) {
									if (scaleOrientation === SCALE_ORIENTATION[0]) {
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
										currXScale = SCALE_TYPES[0];
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
										currYScale = SCALE_TYPES[0];
									}
								} else {
									if (scaleOrientation === SCALE_ORIENTATION[0]) {
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
										currXScale = SCALE_TYPES[1];
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
										currYScale = SCALE_TYPES[1];
									}
								}
								redraw(currXScale, currYScale);
							}
						}, 50);
						return function(chartType, json) {
							json.threadName = json.threadName.match(getThreadNamePattern())[0];
							var loadType = json.threadName;
							//
							var parsedValue = parsePerfAvgLogEvent(chartType, json.message.formattedMessage);
							//
							var isFound = false;
							data.forEach(function(d) {
								if (d.loadType === loadType) {
									isFound = true;
									d.currentRunMetricsPeriodSec += parseInt(runMetricsPeriodSec);
									d.charts.forEach(function(c, i) {
										if (c.values.length === CRITICAL_DOTS_COUNT) {
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
							while (isTimeLimitReached(x.domain()[x.domain().length - 1], currTimeUnit)) {
								if (currTimeUnit.next === null) {
									return;
								}
								currTimeUnit = TIME_LIMITATIONS[currTimeUnit.next];
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
						var updateFunction = drawCharts(data, "Connection count", "Rate[s^-1]", "#tp-" + runId.split(".").join("_"));
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
						var updateFunction = drawCharts(data, "Connection count", "Rate[MB*s^-1]", "#bw-" + runId.split(".").join("_"));
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
