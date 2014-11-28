$(document).ready(function() {

	var VALUE_RUN_MODE_CLIENT = "client";
	var VALUE_RUN_MODE_STANDALONE = "standalone";
	var VALUE_RUN_MODE_SERVER = "server";
	var VALUE_RUN_MODE_WSMOCK = "wsmock";
	var runModes = [VALUE_RUN_MODE_CLIENT, VALUE_RUN_MODE_STANDALONE, VALUE_RUN_MODE_SERVER, VALUE_RUN_MODE_WSMOCK];
	var COUNT_OF_RECORDS = 2050;

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

	$('#run-parameters a[href="#runTimeTab"]').click(function() {
		$("#run\\.time").val(document.getElementById("run.time").defaultValue);
		$("#runTimeSelect").val($("#runTimeSelect option:first").val());
	});

	$('#run-parameters a[href="#dataCountTab"]').click(function() {
		$("#data\\.count").val(document.getElementById("data.count").defaultValue);
	});

	$("#scenario\\.chain\\.load\\.duplicate").keyup(function() {
		$("#scenario\\.chain\\.load").val($(this).val());
	});

	$("#scenario\\.chain\\.load").keyup(function() {
		$("#scenario\\.chain\\.load\\.duplicate").val($(this).val());
	});

	// Save data node in the list
	$(document).on("click", "#save", function() {
		$(".storages").append(appendBlock("storage.addrs", $("#data-node-text").val()));
		$("#data-node-text").val("");
		$(".data-node").hide();
	});

	// Save Driver in the list
	$(document).on("click", "#save-driver", function() {
		$(".drivers").append(appendBlock("remote.servers", $("#driver-text").val()));
		$("#driver-text").val("");
		$(".driver").hide();
	});

	// Remove storage or driver from list
	$(document).on("click", ".remove", function() {
		var elements;
		if ($(this).hasClass("remove-driver")) {
			elements = $(".remote\\.servers:checked");
		} else {
			elements = $(".storage\\.addrs:checked");
		}
		if (confirm("Are you sure? " + elements.size() + " will be deleted") === true) {
			elements.each(function() {
				$(this).parent().parent().remove();
			});
		} else {
			//	do nothing
		}
	});

	//	select change
	$("select").bind("keydown change", function() {
		$('a[href="#' + $(this).val() + '"]').tab('show');
    });

	// functions
	function initComponents() {
		$(".driver").hide();
		$(".data-node").hide();
		configureWebSocket().connect();
	}

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
					<input class=' + key + ' type="checkbox" name=' + key +' value=' + value + '>\
				</span>\
				<label class="form-control">' +
					value +
				'</label>\
			</div>';
		return html;
	}

	// Start mongoose
	$(document).on('submit', '#mainForm',  function(e) {
		e.preventDefault();
		onStartButtonPressed();
	});

	function onStartButtonPressed() {
		$.post("/start", $("#mainForm").serialize(), function(data, status) {
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
				location.reload();
			}
		});
	}

	// Stop mongoose
	$(".stop").click(function() {
		var currentButton = $(this);
		var currentRunId = $(this).parent().parent().attr("id").split("_").join(".");
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
		if (confirm("Are you sure? This action will stop Mongoose which is running on this run.id") === true) {
			$.post("/stop", { "run.id" : currentRunId.split("_").join("."), "type" : "remove" }, function() {
				$("#" + currentRunId).remove();
				currentElement.parent().remove();
				$('a[href="#configuration"]').tab('show');
			});
		}
	});

	// Clear logs content
	$(".clear").click(function() {
		$(this).parent().find("tbody tr").remove();
	});

});