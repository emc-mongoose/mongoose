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
		confMenuController.run(props);
		//
		$.post("/state", function(response) {
			$.each(response, function(index, runId) {
				if(runIdArray.indexOf(runId) < 0) {
					runIdArray.push(runId);
					renderTabByRunId(runId);
				}
			});
			//
			webSocketController.start(props);
		});
	}
	//
	function renderTabByRunId(runId) {
		var runIdText = runId;
		var newId = runId.replace(/\./g, "_");
		var files= [
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
				"text": "Latency[s]"
			}, {
				"id": "dur-" + newId,
				"text": "Duration[s]"
			}
		];
		//
		var run = {
			runId: newId,
			runIdText: runIdText,
			files: files,
			charts: charts
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
			.insertAdjacentHTML("afterbegin", navbar);
	}
	//
	function renderTabs() {
		var tabs = Handlebars.compile(tabsTemplate);
		document.querySelector("#app")
			.insertAdjacentHTML("afterbegin", tabs());
	}
	//
	return {
		run: run
	};
});