$(document).ready(function() {

	$.each(propertiesMap, function(key, value) {
		alert(key);
	});
	var VALUE_RUN_MODE_CLIENT = "client";
	var VALUE_RUN_MODE_STANDALONE = "standalone";
	var VALUE_RUN_MODE_SERVER = "server";
	var VALUE_RUN_MODE_WSMOCK = "wsmock";
	var runModes = [VALUE_RUN_MODE_CLIENT, VALUE_RUN_MODE_STANDALONE, VALUE_RUN_MODE_SERVER, VALUE_RUN_MODE_WSMOCK];
	var COUNT_OF_RECORDS = 2050;

    $('select').on('change', function() {
		var valueSelected = this.value;
		$("#run-mode").val(valueSelected);
    });

	initComponents();
	//excludeDuplicateOptions();

	// functions
	function initComponents() {
		configureWebSocket().connect();
	}

	/*function excludeDuplicateOptions() {
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
	}*/

	function configureWebSocket() {
		var webSocketServer = {
			connect: function() {
				var location = "ws://localhost:8080/logs";
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

	// Start mongoose
	$("#start").click(function(e) {
		alert($("#main-form").serialize());
		e.preventDefault();
		onStartButtonPressed();
	});

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


	/*border*/
	$('#configuration-content').children().hide();

    	$(document).on('click', '.breadcrumb ul a', function() {
    		var sameElement = $(this).attr("href");
    		var element = $(".folders a[href='" + sameElement + "']");
    		if (!element.length) {
    			element = $(".folders label[for='" + sameElement.replace("#", '') + "'");
    		}
    		element.trigger('click');
    	});

    	$(".folders a, .folders label").click(function() {
    		resetParams();
    		$($(this).attr("href")).show();
    		var childrenFolders = "";
    		var childrenDocuments = "";
    		var parentsArray = $(this).parent().parents("li").find("label:first");
    		parentsArray.each(function() {
    			childrenFolders = $(this).siblings("ul").find("label");
    			childrenDocuments = $(this).siblings("ul").children(".file");
    			$(".breadcrumb").append(appendBreadcrumb($(this), childrenFolders, childrenDocuments));
    		});
    		childrenFolders = $(this).siblings("ul").find("label");
    		childrenDocuments = $(this).siblings("ul").children(".file");
    		$(".breadcrumb").append(appendBreadcrumb($(this), childrenFolders, childrenDocuments));
    		$(this).css("color", "#CC0033");
    		$("a[href='#" + $(this).text() + "']").css("color", "#CC0033");
    	});

    	function resetParams() {
    		$("a, label").css("color", "");
    		$(".breadcrumb").empty();
    		$("#configuration-content").children().hide();
    	}

});

function appendBreadcrumb(element, childrenFolders, childrenDocuments) {
	var htmlString = "";
	if (!childrenFolders.length && !childrenDocuments.length) {
		htmlString = "<li class='active'>" + element.text() + "</li>";
	} else {
		var dropDownString = "";
		childrenFolders.each(function() {
			dropDownString += "<li><a tabindex='-1' href='#" + $(this).text() + "'><img class='dropdown-image' src='../images/folder.png'>" + $(this).text() + "</a></li>";
		});

		if (childrenFolders.length) {
			dropDownString += "<hr/>";
		}

		childrenDocuments.each(function() {
			dropDownString += "<li><a tabindex='-1' href='#" + $(this).text() + "'><img class='dropdown-image' src='../images/document.png'>" + $(this).text() + "</a></li>";
		});
		htmlString = "<li class='dropdown open'>\
						<a class='dropdown-toggle' data-toggle='dropdown' href='#'>" + element.text() + "</a>\
						<ul class='dropdown-menu'>" + dropDownString + "</ul>\
					</li>";
	}
	return htmlString;
}

function start(param) {
	alert(param);
}