define([
	"jquery",
	"handlebars",
	"text!../../../templates/run/tab-header.hbs",
	"text!../../../templates/run/tab-content.hbs"
], function(
	$,
	Handlebars,
    tabHeaderTemplate,
    tabContentTemplate
) {
	var runIdArray = [];
	//
	function start() {
		var runId = document.getElementById("duplicate-run.id");
		runId.value = runId.defaultValue;
		onStartButtonPressed();
	}
	//
	function onStartButtonPressed() {
		$.post("/start", $("#main-form").serialize(), function(data, status) {
			if (data) {
				if (confirm("Are you sure? " + data) === true) {
					$.post("/stop", { "run.id" : $("#run\\.id").find("input").val(), "type" : "remove" },
						function(data, status) {
							if (status) {
								onStartButtonPressed();
							}
						}).fail(function() {
							alert("Internal Server Error");
						});
				}
			} else {
				$('#config-type').find('option').prop('selected', function() {
					return this.defaultSelected;
				});
				//
				$.post("/state", function(response) {
					$.each(response, function(index, runId) {
						if(runIdArray.indexOf(runId) < 0) {
							runIdArray.push(runId);
							renderTabByRunId(runId);
						}
					});
				});
			}
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
	return {
		start: start
	};
});