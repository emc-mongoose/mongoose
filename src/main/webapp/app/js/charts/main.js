define([
	"jquery",
	"../util/constants",
	"../charts/util/common",
	"../charts/scenarios/single",
	"../charts/scenarios/chain",
	"../charts/scenarios/rampup"
], function(
	$, constants, common, single, chain, rampup
) {

	var avg = constants.getAvgConstant(),
		last = constants.getLastConstant(),
		min = constants.getMinConstant(),
		max = constants.getMaxConstant();
	var chartsArray;

	function charts(array) {
		chartsArray = array;
		//
		return {
			single: drawSingleCharts,
			chain: drawChainCharts,
			rampup: drawRampupCharts
		}
	}

	function drawSingleCharts(jsonValue) {
		var temp = (Array.isArray(jsonValue)) ? jsonValue[0] : jsonValue;
		//  conf params for single charts
		var runId = temp.contextMap[constants.getCfgConstants().runId],
			runScenarioName = temp.contextMap[constants.getCfgConstants().runScenarioName],
			runMetricsPeriodSec = parseInt(
				temp.contextMap[constants.getCfgConstants().runMetricsPeriodSec]
			);
		last.text = "last " + runMetricsPeriodSec + " sec";

		var tpAndBwData = [
			{
				name: avg,
				values: [
					{x: 0, y: 0}
				]
			}, {
				name: last,
				values: [
					{x: 0, y: 0}
				]
			}
		];

		var latAndDurData = [
			{
				name: avg,
				values: [
					{x: 0, y: 0}
				]
			}, {
				name: min,
				values: [
					{x: 0, y: 0}
				]
			}, {
				name: max,
				values: [
					{x: 0, y: 0}
				]
			}
		];

		var throughput = $.extend(true, [], tpAndBwData),
			bandwidth = $.extend(true, [], tpAndBwData),
			latency = $.extend(true, [], latAndDurData),
			duration = $.extend(true, [], latAndDurData);

		if ((jsonValue !== undefined) && (jsonValue.length > 0)) {
			single.clearArrays(throughput);
			single.clearArrays(bandwidth);
			single.clearArrays(latency);
			single.clearArrays(duration);
			var tpSec = single.initDataArray(
				throughput, jsonValue, constants.getChartTypes().TP, runMetricsPeriodSec
			);
			var bwSec = single.initDataArray(
				bandwidth, jsonValue, constants.getChartTypes().BW, runMetricsPeriodSec
			);
			var latSec = single.initDataArray(
				latency, jsonValue, constants.getChartTypes().LAT, runMetricsPeriodSec
			);
			var durSec = single.initDataArray(
				duration, jsonValue, constants.getChartTypes().DUR, runMetricsPeriodSec
			);
			chartsArray.push(common.getScenarioChartObject(runId, runScenarioName,
				[single.drawThroughputCharts(throughput, jsonValue[0], tpSec),
					single.drawBandwidthCharts(bandwidth, jsonValue[0], bwSec),
						single.drawLatencyCharts(latency, jsonValue[0], latSec),
							single.drawDurationCharts(duration, jsonValue[0], durSec)]));
		} else {
			//
			chartsArray.push(common.getScenarioChartObject(runId, runScenarioName,
				[single.drawThroughputCharts(throughput, jsonValue),
					single.drawBandwidthCharts(bandwidth, jsonValue),
						single.drawLatencyCharts(latency, jsonValue),
							single.drawDurationCharts(duration, jsonValue)]));
		}
	}

	function drawChainCharts(runId, runMetricsPeriodSec, loadType, array) {
		last.text = "last " + runMetricsPeriodSec + " sec";
		//
		var data = [
			{
				loadType: loadType,
				charts: [
					{
						name: avg,
						values: [
							{x: 0, y: 0}
						]
					}, {
						name: last,
						values: [
							{x: 0, y: 0}
						]
					}
				],
				currentRunMetricsPeriodSec: 0
			}
		];

		var latencyData = [
			{
				loadType: loadType,
				charts: [
					{
						name: avg,
						values: [
							{x: 0, y: 0}
						]
					}, {
						name: min,
						values: [
							{x: 0, y: 0}
						]
					}, {
						name: max,
						values: [
							{x: 0, y: 0}
						]
					}
				],
				currentRunMetricsPeriodSec: 0
			}
		];

		var throughput = $.extend(true, [], data),
			bandwidth = $.extend(true, [], data),
			latency = $.extend(true, [], latencyData),
			duration = $.extend(true, [], latencyData);
		//
		if ((array !== undefined) && (array.length > 0)) {
			chain.clearArrays(throughput, runMetricsPeriodSec);
			chain.clearArrays(bandwidth, runMetricsPeriodSec);
			chain.clearArrays(latency, runMetricsPeriodSec);
			chain.clearArrays(duration, runMetricsPeriodSec);
			//
			chain.initDataArray(throughput, array, constants.getChartTypes().TP, runMetricsPeriodSec);
			chain.initDataArray(bandwidth, array, constants.getChartTypes().BW, runMetricsPeriodSec);
			chain.initDataArray(latency, array, constants.getChartTypes().LAT, runMetricsPeriodSec);
			chain.initDataArray(duration, array, constants.getChartTypes().DUR, runMetricsPeriodSec);
		}
		//
		chartsArray.push({
			"run.id": runId,
			"run.scenario.name": "chain",
			"charts": [
				chain.drawThroughputChart(throughput, runId, runMetricsPeriodSec),
				chain.drawBandwidthChart(bandwidth, runId, runMetricsPeriodSec),
				chain.drawLatencyChart(latency, runId, runMetricsPeriodSec),
				chain.drawDurationChart(duration, runId, runMetricsPeriodSec)
			]
		});
	}

	function drawRampupCharts(runId, scenarioChainLoad, rampupConnCounts, loadRampupSizes) {
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
			"run.scenario.name": "rampup",
			"charts": [
				rampup.drawThroughputCharts(runId, loadTypes, loadRampupSizesArray, rampupConnCountsArray),
				rampup.drawBandwidthCharts(runId, loadTypes, loadRampupSizesArray, rampupConnCountsArray)
			]
		});
	}

	return {
		charts: charts
	};

});