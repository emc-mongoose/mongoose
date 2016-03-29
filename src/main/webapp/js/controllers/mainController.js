define([
	"jquery",
	"handlebars",
	"./configuration/confMenuController",
	"./websockets/webSocketController",
	"text!../../templates/navbar.hbs",
	"text!../../templates/tabs.hbs",
	"text!../../templates/run/tab-header.hbs",
	"text!../../templates/run/tab-content.hbs"
], function(
	$,
	Handlebars,
	confMenuController,
	webSocketController,
	navbarTemplate,
	tabsTemplate,
	tabHeaderTemplate,
	tabContentTemplate
) {
	var runIdArray = [];
	//
	function run(props) {
		//  render navbar and tabs before any other interactions
		render(props);
		confMenuController.run(props, runIdArray);
		//
		$.get("/stop", function(stopped) {
			$.get("/state", function(response) {
				$.each(response, function(index, runId) {
					if(runIdArray.indexOf(runId) < 0) {
						runIdArray.push(runId);
						if(stopped[runId]) {
							renderTabByRunId(runId, false);
						} else {
							renderTabByRunId(runId, true);
						}
						bindEvents(runId);
					}
				});
				//
				webSocketController.start(props);
			});
		});
	}
	//
	function renderTabByRunId(runId, active) {
		var runIdText = runId;
		var newId = runId.replace(/\./g, "_");
		var tables = [
			{
				"id": newId + "-messages-csv",
				"text": "messages.csv",
				"active": true
			}, {
				"id": newId + "-errors-log",
				"text": "errors.log"
			}, {
				"id": newId + "-perf-avg-csv",
				"text": "perf.avg.csv"
			}, {
				"id": newId + "-perf-sum-csv",
				"text": "perf.sum.csv"
			}
		];
		//
		var charts = [
			{
				"id": "tp-" + newId,
				"text": "Throughput[obj/s]",
				"active": true
			}, {
				"id": "bw-" + newId,
				"text": "Bandwidth[mb/s]"
			}, {
				"id": "lat-" + newId,
				"text": "Latency[us]"
			}, {
				"id": "dur-" + newId,
				"text": "Duration[us]"
			}
		];
		//
		var run = {
			runId: newId,
			runIdText: runIdText,
			tables: tables,
			charts: charts,
			active: active
		};
		var ul = $(".scenario-tabs");
		//  render tab header
		var compiled = Handlebars.compile(tabHeaderTemplate);
		var html = compiled(run);
		ul.append(html);
		//  render tab content
		var div = $(".scenarios-content");
		compiled = Handlebars.compile(tabContentTemplate);
		html = compiled(run);
		div.append(html);
	}

	function bindEvents(runId) {
		//
		var element = $("#" + runId.replace(/\./g, "_") + "-tab");
		element.find(".stop").click(function() {
			var currentRunId = $(this).val();
			var currentButton = $(this);
			currentButton.remove();
			$.post("/stop", {
				"run.id" : currentRunId,
				"type" : "stop"
			}, function() {
				//  do nothing
			}).fail(function() {
				alert("Internal Server Error");
			});
		});

		element.find(".kill").click(function() {
			var currentButton = $(this);
			var currentRunId = $(this).attr("value");
			if (confirm("Please note that the test will be shut down if it's running.") === true) {
				currentButton.remove();
				$("#" + currentRunId.replace(/\./g, "_") + "-tab").remove();
				$('a[href="#' + currentRunId.replace(/\./g, "_") + '-tab"]').remove();
				$('a[href="#configuration"]').tab('show');
				$.post("/stop", {
					"run.id" : currentRunId,
					"type" : "remove"
				}, function() {});
			}
		});
	}

	//
	function render(props) {
		renderNavbar(props.run.version || "unknown");
		renderTabs();
	}
	//
	function renderNavbar(runVersion) {
		var run = {
			version: runVersion
		};
		//
		var compiled = Handlebars.compile(navbarTemplate);
		var navbar = compiled(run);
		document.querySelector("body")
			.insertAdjacentHTML('afterbegin', navbar);
	}
	//
	function renderTabs() {
		var tabs = Handlebars.compile(tabsTemplate);
		document.querySelector("#app")
			.insertAdjacentHTML('afterbegin', tabs());
	}
	//
	return {
		run: run
	};
});