$(document).ready(function() {
	//
	excludeDuplicateOptions();
	//
	var shortPropsMap = {};
	var ul = $(".folders");
	var WEBSOCKET_URL = "ws://" + window.location.host + "/logs";
	var COUNT_OF_RECORDS = 2050;

	walkTreeMap(propertiesMap, ul, shortPropsMap);
	buildDivBlocksByFileNames(shortPropsMap);
	generatePropertyPage();
	$(".folders").hide();
	//
	$("select").each(function() {
		var valueSelected = this.value;
		var notSelected = $("option:not(:selected)", this);
		notSelected.each(function() {
			if ($("#" + $(this).val()).is("div")) {
				$("#" + $(this).val()).hide();
			}
		});
	});

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

	$("#fake-run\\.scenario\\.name").on("change", function() {
		var valueSelected = this.value;
		$("#scenario-button").attr("data-target", "#" + valueSelected);
	});

	$("#fake-storage\\.api").on("change", function() {
		var valueSelected = this.value;
		$("#api-button").attr("data-target", "#" + valueSelected);
	});

	$("#run\\.time").change(function() {
		$("#data\\.count").val(document.getElementById("data.count").defaultValue);
	});

	$("#data\\.count").change(function() {
		$("#run\\.time").val(document.getElementById("run.time").defaultValue);
	});

	$("#base input, #base select").on("change", function() {
		var currElement = $(this);
		if (currElement.parents(".complex").length === 1) {
			var input = $("#fake-run\\.time\\.input").val();
			var select = $("#fake-run\\.time\\.select").val();
			currElement = $("#fake-run\\.time").val(input + "." + select);
		}
		if (currElement.is("select")) {
			var valueSelected = currElement.children("option").filter(":selected").text();
			$('select[pointer="'+currElement.attr("pointer")+'"]').val(currElement.val());
			var element = document.getElementById(currElement.attr("pointer"));
			if (element) {
				element.value = valueSelected;
			}
		} else {
			$('input[pointer="'+currElement.attr("pointer")+'"]').val(currElement.val());
			var element = document.getElementById(currElement.attr("pointer"));
			if (element) {
				element.value = currElement.val();
			}
		}
	});

	$("#extended input").on("change", function() {
		$('input[pointer="' + $(this).attr("id") + '"]').val($(this).val());
	});

	configureWebSocket(WEBSOCKET_URL, COUNT_OF_RECORDS).connect();

	/*$('a[href="#remote"]').hide();
	$("#select").on("change", function() {
		var valueSelected = this.value;
		if (valueSelected === "client") {
			$('a[href="#remote"]').show();
		} else {
			if ($("#remote").is(":visible")) {
				$(".breadcrumb").empty();
				generatePropertyPage();
			}
			$('a[href="#remote"]').hide();
		}
		$("#run-mode").val(valueSelected);
	});*/

	$("#start").click(function(e) {
		e.preventDefault();
		onStartButtonPressed();
	});

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

	$(".clear").click(function() {
		$(this).parent().find("tbody tr").remove();
	});


	$(document).on('click', '.breadcrumb ul a', function(e) {
		e.preventDefault();
		var sameElement = $(this).attr("href").replace("#", "");
		var element = $(".folders a[href='#" + sameElement + "']");
		if (!element.length) {
			element = $('label[for="' + sameElement + '"]');
		}
		element.trigger('click');
	});

	$(".folders a, .folders label").click(function(e) {
		if ($(this).is("a")) {
			e.preventDefault();
		}
		onFoldersElementClick($(this));
	});

	$("#chain-load").click(function() {
		$("#fake-chain").modal('show').css("z-index", 5000);
	});
});

function generatePropertyPage() {
	if (!$("#properties").is(":checked")) {
		$("#properties").trigger("click");
	}
	onFoldersElementClick($('a[href="#auth"]'));
}

function onFoldersElementClick(element) {
	resetParams();
	$($(element).attr("href")).show();
	var childrenFolders = "";
	var childrenDocuments = "";
	var parentsArray = $(element).parent().parents("li").find("label:first");
	parentsArray.each(function() {
		childrenFolders = $(this).siblings("ul").find("label");
		childrenDocuments = $(this).siblings("ul").children(".file");
		$(".breadcrumb").append(appendBreadcrumb($(this), childrenFolders, childrenDocuments));
	});
	childrenFolders = $(element).siblings("ul").find("label");
	childrenDocuments = $(element).siblings("ul").children(".file");
	$(".breadcrumb").append(appendBreadcrumb($(element), childrenFolders, childrenDocuments));
	$(element).css("color", "#CC0033");
	$("a[href='#" + $(element).text() + "']").css("color", "#CC0033");
}

function walkTreeMap(map, ul, shortsPropsMap) {
	$.each(map, function(key, value) {
		var element;
		if (jQuery.isArray(value)) {
			element = ul.addChild("<li>")
					.addClass("file")
					.addChild("<a>")
					.attr("href", "#" + key)
					.text(key);
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
				if (obj[i].key === "data.count") {
					placeHolder = "If you set this property, 'run.time' will reset to default";
				} else if (obj[i].key === "run.time") {
					placeHolder = "If you set this property, 'data.count' will reset to default";
				} else if (obj[i].key === "data.src.fpath") {
					placeHolder = "Format: log/<run.mode>/<run.id>/<filename>";
				}
				formGroupDiv.append($("<label>", {
							for: obj[i].key,
							class: "col-sm-2 control-label",
							text: obj[i].value.key
						}))
						.append($("<div>", {
							class: "col-sm-10"
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

function configureWebSocket(location, countOfRecords) {
	var webSocketServer = {
		connect: function() {
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
						if ($("#"+entry+"errors-log table tbody tr").length > countOfRecords) {
							$("#"+entry+"errors-log table tbody tr:first-child").remove();
						}
						$("#"+entry+"errors-log table tbody").append(appendStringToTable(json));
						break;
					case "msg":
						if ($("#"+entry+"messages-csv table tbody tr").length > countOfRecords) {
							$("#"+entry+"messages-csv table tbody tr:first-child").remove();
						}
						$("#"+entry+"messages-csv table tbody").append(appendStringToTable(json));
						break;
					case "perfSum":
						if ($("#"+entry+"perf-sum-csv table tbody tr").length > countOfRecords) {
							$("#"+entry+"perf-sum-csv table tbody tr:first-child").remove();
						}
						$("#"+entry+"perf-sum-csv table tbody").append(appendStringToTable(json));
						break;
					case "perfAvg":
						if ($("#"+entry+"perf-avg-csv table tbody tr").length > countOfRecords) {
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

function resetParams() {
	$("a, label").css("color", "");
	$(".breadcrumb").empty();
	$("#configuration-content").children().hide();
}

function appendBreadcrumb(element, childrenFolders, childrenDocuments) {
	var html;
	var folderImagePath = "../images/folder.png";
	var documentImagePath = "../images/document.png";

	if (!childrenFolders.length && !childrenDocuments.length) {
		html = $("<li>").attr({
			class: "active",
		}).text(element.text());
	} else {
		var dropDownList = $("<ul>").addClass("dropdown-menu");
		if (childrenFolders.length) {
			iterateChildren(dropDownList, childrenFolders, folderImagePath);
			dropDownList.append("<hr/>");
		}
		iterateChildren(dropDownList, childrenDocuments, documentImagePath);

		html = $("<li>").addClass("dropdown")
						.append($("<a>", {
							class: "dropdown-toggle",
							"data-toggle": "dropdown",
							href: "#",
							text: element.text()
						}))
						.append(dropDownList);
    }
    return html;
}

function iterateChildren(dropDownList, children, image) {
	children.each(function() {
		var currElement = $(this);
		dropDownList.addChild("<li>")
					.addChild($("<a>", {
						tabindex: -1,
						href: "#" + currElement.text()
					}))
					.append($("<img>", {
						class: "dropdown-image",
						src: image
					}))
					.append($("<span>").text(currElement.text()));
	});
}