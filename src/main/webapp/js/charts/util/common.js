define([
	"jquery",
	"../../util/constants"
], function(
	$,
	constants
) {
	var margin = {top: 40, right: 200, bottom: 60, left: 60},
		width = 1070 - margin.left - margin.right,
		height = 460 - margin.top - margin.bottom;

	function parsePerfAvgLogEvent(chartType, value) {
		var pattern = null;
		var chartTypes = constants.getChartTypes();
		switch(chartType) {
			case chartTypes.TP:
				pattern = "[\\s]+TP\\[op/s]=\\(([\\.\\d]+)/([\\.\\d]+)\\)";
				break;
			case chartTypes.BW:
				pattern = "[\\s]+BW\\[MB/s]=\\(([\\.\\d]+)/([\\.\\d]+)\\)";
				break;
			case chartTypes.LAT:
				pattern = "[\\s]+latency\\[us\\]=\\((\\d+)/(\\d+)/(\\d+)\\)";
				break;
			case chartTypes.DUR:
				pattern = "[\\s]+duration\\[us\\]=\\((\\d+)/(\\d+)/(\\d+)\\)";
				break;
		}
		var matched = value.match(pattern);
		return matched.slice(1, matched.length.length);
	}

	function getThreadNamePattern() {
		return /([\d]+)-([A-Za-z0-9]+)-([CreateRdDlUpAn]+)[\d]*-([\d]*)x([\d]*)x?([\d]*)/gi;
	}

	function isTimeLimitReached(domainMaxValue, currTimeUnit) {
		return domainMaxValue >= currTimeUnit.limit;
	}

	function getScenarioChartObject(runId, runScenarioName, scenarioCharts) {
		return {
			"run.id": runId,
			"run.scenario.name": runScenarioName,
			"charts": scenarioCharts
		};
	}

	function saveChart(chartDOMPath, w, h) {
		$.strRemove = function(theTarget, theString) {
			return $("<div/>").append(
				$(theTarget, theString).remove().end()
			).html();
		};
		//
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
		var imgSrc = 'data:image/svg+xml;base64,' + btoa(theResult);
		var canvas = document.createElement("canvas");
		canvas.setAttribute("width", w);
		canvas.setAttribute("height", h);

		var context = canvas.getContext("2d");

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

	//  linear and log scales
	function appendScaleLabels(svg, chartEntry, addHeight) {
		var scales = constants.getScales();
		var scaleTypes = constants.getScaleTypes();
		//
		var groups = svg.selectAll(".scale-labels")
			.data(scales);
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
			.on("click", function() {
				var currScaleType = null;
				if (d3.select(this).property("checked")) {
					currScaleType = scaleTypes[1];
				} else {
					d3.select(this).property("checked", false);
					currScaleType = scaleTypes[0];
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
			.text(scaleTypes[0]);
	}

	return {
		parsePerfAvgLogEvent: parsePerfAvgLogEvent,
		getThreadNamePattern: getThreadNamePattern,
		isTimeLimitReached: isTimeLimitReached,
		getScenarioChartObject: getScenarioChartObject,
		appendScaleLabels: appendScaleLabels,
		saveChart: saveChart,
		getMargin: function() {
			return margin;
		},
		getWidth: function() {
			return width;
		},
		getHeight: function() {
			return height;
		}
	};

});