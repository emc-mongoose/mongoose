$(document).ready(function() {
	var WEBSOCKET_URL = "ws://" + window.location.host + "/logs";
	var TABLE_ROWS_COUNT = 2050;
	excludeDuplicateOptions();
	//
	var shortPropsMap = {};
	var ul = $(".folders");
	//
	walkTreeMap(propertiesMap, ul, shortPropsMap);
	buildDivBlocksByFileNames(shortPropsMap);
	generatePropertyPage();
	configureWebSocketConnection(WEBSOCKET_URL, TABLE_ROWS_COUNT).connect();
	//
	//  Prevent default page scrolling
	/*$('.scrollable').bind('mousewheel DOMMouseScroll', function (e) {
		var e0 = e.originalEvent,
			delta = e0.wheelDelta || -e0.detail;

		this.scrollTop += (delta < 0 ? 1 : -1) * 30;
		e.preventDefault();
	});*/
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

	$("#config-type").on("change", function() {
		var valueSelected = this.value;
		if (valueSelected === "base") {
			$(".folders").hide();
		} else {
			$(".folders").show();
		}
	});

	$("#run-modes select").on("change", function() {
		var valueSelected = this.value;
		$("#run-mode").val(valueSelected);
	});

	$("#backup-run\\.scenario\\.name").on("change", function() {
		var valueSelected = this.value;
		$("#scenario-button").attr("data-target", "#" + valueSelected);
	});

	$("#backup-storage\\.api").on("change", function() {
		var valueSelected = this.value;
		$("#api-button").attr("data-target", "#" + valueSelected);
	});

	$(".complex").change(function() {
		$("#backup-data\\.count").val(document.getElementById("backup-data.count").defaultValue);
		$("#data\\.count").val(document.getElementById("data.count").defaultValue);
	});

	$("#backup-data\\.count").change(function() {
		$(".complex input").val($(".complex input").get(0).defaultValue);
		$(".complex select").val($(".complex select option:first").val());
		$("#run\\.time").val(document.getElementById("run.time").defaultValue);
	});

	$("#base input, #base select").on("change", function() {
		var currElement = $(this);
		if (currElement.parents(".complex").length === 1) {
			var input = $("#backup-run\\.time\\.input").val();
			var select = $("#backup-run\\.time\\.select").val();
			currElement = $("#backup-run\\.time").val(input + "." + select);
		}
		//
		if (currElement.is("select")) {
			var valueSelected = currElement.children("option").filter(":selected").text().trim();
			$('select[data-pointer="'+currElement.attr("data-pointer")+'"]').val(currElement.val());
			var element = document.getElementById(currElement.attr("data-pointer"));
			if (element) {
				element.value = valueSelected;
			}
		} else {
			$('input[data-pointer="' + currElement.attr("data-pointer") + '"]').val(currElement.val());
			var element = document.getElementById(currElement.attr("data-pointer"));
			if (element) {
				element.value = currElement.val();
			}
		}
	});

	$("#extended input").on("change", function() {
		if (($(this).attr("id") === "run.time") || ($(this).attr("id") === "data.count")) {
			return;
		}
		$('input[data-pointer="' + $(this).attr("id") + '"]').val($(this).val());
		$('select[data-pointer="' + $(this).attr("id") + '"] option:contains(' + $(this).val() + ')')
			.attr('selected', 'selected');
	});

	$("#start").click(function(e) {
		e.preventDefault();
		var runId = document.getElementById("backup-run.id");
		runId.value = runId.defaultValue;
		onStartButtonPressed();
	});

	$(".stop").click(function() {
		var currentButton = $(this);
		var currentRunId = $(this).parent().parent().attr("tab-id");
		$.post("/stop", { "run.id" : currentRunId, "type" : "stop" }, function() {
			currentButton.remove();
		}).fail(function() {
			alert("Internal Server Error");
			currentButton.remove();
		});
	});

	$(".glyphicon-remove").click(function() {
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

	$(".clear").click(function() {
		$(this).parent().find("tbody tr").remove();
	});

	$(".folders a, .folders label").click(function(e) {
		if ($(this).is("a")) {
			e.preventDefault();
		}
		//
		onMenuItemClick($(this));
	});

	$("#chain-load").click(function() {
		$("#backup-chain").modal('show').css("z-index", 5000);
	});

	$("#save-config").click(function() {
		$.post("/save", $("#main-form").serialize(), function(data, status) {
			alert("Config was successfully saved");
		});
	});

	$("#save-file").click(function(e) {
		e.preventDefault();
		$.get("/save", $("#main-form").serialize(), function(data, status) {
			window.location.href = "/save/config.txt";
		});
	});

	$("#config-file").change(function() {
		var input = $(this).get(0);
		loadPropertiesFromFile(input.files[0]);
	});

	$("#file-checkbox").change(function() {
		if ($(this).is(":checked")) {
			$("#config-file").show();
		} else {
			$("#config-file").hide();
		}
	});

	$(".property").click(function() {
		var id = $(this).attr("href").replace(/\./g, "\\.");
		var name = $(this).parents(".file").find(".props").text();
		$("#" + name).show();
		var element = $("#" + name).find($(id));
		var parent = element.parents(".form-group");
		$("#" + name).children().hide();
		parent.show();
	});

});

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

jQuery.fn.addChild = function(html) {
	var target = $(this[0]);
	var child = $(html);
	child.appendTo(target);
	return child;
};

jQuery.fn.prependChild = function(html) {
	var target = $(this[0]);
	var child = $(html);
	child.prependTo(target);
	return child;
};

function generatePropertyPage() {
	if (!$("#properties").is(":checked")) {
		$("#properties").trigger("click");
	}
	onMenuItemClick($('a[href="#auth"]'));
}

function onMenuItemClick(element) {
	resetParams();
	element.css("color", "#CC0033");
	if (element.is("a")) {
		var block = $(element.attr("href"));
		block.show();
		block.children().show();
	}
}

function resetParams() {
	$("a, label").css("color", "");
	$("#configuration-content").children().hide();
}

function configureWebSocketConnection(location, countOfRecords) {
	var MARKERS = {
		ERR: "err",
		MSG: "msg",
		PERF_SUM: "perfSum",
		PERF_AVG: "perfAvg"
	};
	//
	var LOG_FILES = {
		ERR: "errors-log",
		MSG: "messages-csv",
		PERF_SUM: "perf-sum-csv",
		PERF_AVG: "perf-avg-csv"
	};
	var webSocketServer = {
		connect: function() {
			this.ws = new WebSocket(location);
			//
			this.ws.onopen = function() {
				//  empty
			};
			//
			this.ws.onmessage = function(message) {
				var json = JSON.parse(message.data);
				var entry = json.contextMap["run.id"].split(".").join("_");
				if (!json.hasOwnProperty("marker") || !json.loggerName) {
					return;
				} else if (!json.marker.hasOwnProperty("name")) {
					return;
				}
				//
				switch (json.marker.name) {
					case MARKERS.ERR:
						appendMessageToTable(entry, LOG_FILES.ERR, countOfRecords, json);
						break;
					case MARKERS.MSG:
						appendMessageToTable(entry, LOG_FILES.MSG, countOfRecords, json);
						break;
					case MARKERS.PERF_SUM:
						appendMessageToTable(entry, LOG_FILES.PERF_SUM, countOfRecords, json);
						break;
					case MARKERS.PERF_AVG:
						appendMessageToTable(entry, LOG_FILES.PERF_AVG, countOfRecords, json);
						break;
				}
			};
			//
			this.ws.onclose = function() {
				//  empty
			};
		}
	};
	return webSocketServer;
}

function appendMessageToTable(entry, tableName, countOfRows, message) {
	if ($("#" + entry + tableName + " table tbody tr").length > countOfRows) {
		$("#" + entry + tableName + " table tbody tr:first-child").remove();
	}
	$("#" + entry + tableName +" table tbody").append(getTableRowByMessage(message));
}

//  Fix it later
function getTableRowByMessage(json) {
	html = '<tr>\
			<td class="filterable-cell">' + json.level.standardLevel + '</td>\
			<td class="filterable-cell">' + json.loggerName + '</td>\
			<td class="filterable-cell">' + json.threadName + '</td>\
			<td class="filterable-cell">' + new Date(json.timeMillis) + '</td>\
			<td class="filterable-cell">' + json.message.formattedMessage + '</td>\
			</tr>';
	return html;
}

function onStartButtonPressed() {
	$.post("/start", $("#main-form").serialize(), function(data, status) {
		if (data) {
			if (confirm("Are you sure? " + data) === true) {
				$.post("/stop", { "run.id" : $("#run\\.id").val(), "type" : "remove" }, function(data, status) {
					if (status) {
						onStartButtonPressed();
					}
				}).fail(function() {
					alert("Internal Server Error");
				});
			} else {
				//	do nothing
			}
		} else {
			$('#config-type option').prop('selected', function() {
				return this.defaultSelected;
			});
			location.reload();
		}
	});
}

function loadPropertiesFromFile(file) {
	var reader = new FileReader();
	reader.onload = function(e) {
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
				}
				$('input[data-pointer="' + $(elementId).attr("id") + '"]').val($(elementId).val());
				$('select[data-pointer="' + $(elementId).attr("id") + '"] option:contains(' + $(elementId).val() + ')')
					.attr('selected', 'selected');
			}
		}
	};

	reader.readAsText(file);
}