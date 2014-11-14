$(document).ready(function() {

	var VALUE_RUN_MODE_CLIENT = "VALUE_RUN_MODE_CLIENT";
	var VALUE_RUN_MODE_STANDALONE = "VALUE_RUN_MODE_STANDALONE";
	var VALUE_RUN_MODE_SERVER = "VALUE_RUN_MODE_SERVER";
	var VALUE_RUN_MODE_WSMOCK = "VALUE_RUN_MODE_WSMOCK";
	var runModes = [VALUE_RUN_MODE_CLIENT, VALUE_RUN_MODE_STANDALONE, VALUE_RUN_MODE_SERVER, VALUE_RUN_MODE_WSMOCK];
	var COUNT_OF_RECORDS = 50;

	initComponents();
	excludeDuplicateOptions();

	// Storage Block
	$(".add-node").click(function() {
		if ($(".data-node").is(":visible")) {
			$(".data-node").hide();
		} else {
			$(".data-node").show();
		}
	});

	// Drivers Block
	$(".add-driver").click(function() {
		if ($(".driver").is(":visible")) {
			$(".driver").hide();
		} else {
			$(".driver").show();
		}
	});

	// Save data node in the list
	$(document).on("click", "#save", function() {
		$(".storages").append(appendBlock("dataNodes", $("#data-node-text").val()));
		$("#data-node-text").val("");
		$(".data-node").hide();
	});

	// Save Driver in the list
	$(document).on("click", "#save-driver", function() {
		$(".drivers").append(appendBlock("drivers", $("#driver-text").val()));
		$("#driver-text").val("");
		$(".driver").hide();
	});

	// Remove storage or driver from list
	$(document).on("click", ".remove", function() {
		$(this).parent().parent().remove();
	});

	// RunModes
	$(document).on("click", "#standalone", function() {
		$("#runmode").val(VALUE_RUN_MODE_STANDALONE);
		$(".runmodes .list-group .list-group-item").removeClass("active");
		$(this).addClass("active");
	});

	$(document).on("click", "#distributed", function() {
		$("#runmode").val(VALUE_RUN_MODE_CLIENT);
		$(".runmodes .list-group .list-group-item").removeClass("active");
		$(this).addClass("active");
	});

	$(document).on("click", "#driver", function() {
		$("#runmode").val(VALUE_RUN_MODE_SERVER);
		$(".runmodes .list-group .list-group-item").removeClass("active");
		$(this).addClass("active");
	});

	$(document).on("click", "#wsmock", function() {
		$("#runmode").val(VALUE_RUN_MODE_WSMOCK);
		$(".runmodes .list-group .list-group-item").removeClass("active");
		$(this).addClass("active");
	});

	// functions
	function initComponents() {
		$(".driver").hide();
		$(".data-node").hide();
		$("#runmode").val($.cookie("runmode"));
		configureWebSocket().connect();
	}

	function excludeDuplicateOptions() {
		var found = [];
		$("select option").each(function() {
			if($.inArray(this.value, found) != -1) $(this).remove();
			found.push(this.value);
		});
	}

	function configureWebSocket() {
		var webSocketServer = {
			connect: function() {
				var location = document.location.toString().replace('http://', 'ws://') + "logs";
				this.ws = new WebSocket(location);
				//
				this.ws.onopen = function() {
					// empty
				}
				//
				this.ws.onmessage = function(m) {
					var json = JSON.parse(m.data);
					var entry = json.contextMap["run.id"].split(".").join("_");
					// fix later
					if (!json.message.message) {
						str = json.message.messagePattern.split("{}");
						resultString = "";
						for (s = 0; s < str.length - 1; s++) {
							resultString += str[s]+json.message.stringArgs[s];
						}
						json.message.message = resultString + str[str.length - 1];
					}
					if (!json.hasOwnProperty("marker"))
						return;
					if (!json.marker.hasOwnProperty("name"))
						return;
					//
					switch (json.marker.name) {
						case "err":
							if ($("#"+entry+"errors-log table tbody tr").length > COUNT_OF_RECORDS) {
								$("#"+entry+"errors-log table tbody tr:first-child").remove();
							}
							$("#"+entry+"errors-log table tbody").append(appendStringToTable(json));
							break;
						case "msg":
							if ($("#"+entry+"messages-csv table tbody tr").length > COUNT_OF_RECORDS) {
								$("#"+entry+"messages-csv table tbody tr:first-child").remove();
							}
							$("#"+entry+"messages-csv table tbody").append(appendStringToTable(json));
							break;
						case "perfSum":
							if ($("#"+entry+"perf-sum-csv table tbody tr").length > COUNT_OF_RECORDS) {
								$("#"+entry+"perf-sum-csv table tbody tr:first-child").remove();
							}
							$("#"+entry+"perf-sum-csv table tbody").append(appendStringToTable(json));
							break;
						case "perfAvg":
							if ($("#"+entry+"perf-avg-csv table tbody tr").length > COUNT_OF_RECORDS) {
								$("#"+entry+"perf-avg-csv table tbody tr:first-child").remove();
							}
							$("#"+entry+"perf-avg-csv table tbody").append(appendStringToTable(json));
							break;
					}
				}
				//
				this.ws.onclose = function() {
					this.ws = null;
				}
			}
		};
		return webSocketServer;
	}

	function appendStringToTable(json) {
		html = '<tr>\
			<td class="filterable-cell">' + json.level.name + '</td>\
			<td class="filterable-cell">' + json.loggerName + '</td>\
			<td class="filterable-cell">' + json.marker.name + '</td>\
			<td class="filterable-cell">' + json.threadName + '</td>\
			<td class="filterable-cell">' + new Date(json.timeMillis) + '</td>\
			<td class="filterable-cell">' + json.message.message + '</td>\
			</tr>';
		return html;
	}

	function appendBlock(key, value) {
		html =
			'<div class="input-group">\
				<span class="input-group-addon">\
					<input type="checkbox" name=' + key +' value=' + value + '>\
				</span>\
				<label class="form-control">' +
					value +
				'</label>\
				<span class="input-group-btn">\
					<button type="button" class="btn btn-default remove">Remove</button>\
				</span>\
			</div>';
		return html;
	}

	// Start mongoose
	$(document).on('submit', '#mainForm',  function(e) {
		e.preventDefault();
		$.post("/start", $("#mainForm").serialize(), function(data, status) {
			location.reload();
		});
	});

	// Stop mongoose
	$(".stop").click(function() {
		var currentButton = $(this);
		var currentRunId = $(this).parent().parent().attr("id").split("_").join(".");
		$.post("/stop", { "runid" : currentRunId }, function() {
			currentButton.attr("disabled", "disabled");
			location.reload();
		}).fail(function() {
			currentButton.attr("disabled", "disabled");
			alert("Internal Server Error");
		});
	});

	// Clear logs content
	$(".clear").click(function() {
		$(this).parent().find("tbody tr").remove();
	});

});