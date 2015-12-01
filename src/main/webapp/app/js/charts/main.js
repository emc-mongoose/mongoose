define([
	"jquery",
	"../util/constants",
	"../charts/util/common",
	"../charts/scenarios/single",
	"../charts/scenarios/chain"
], function(
	$, constants, common, single, chain
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
		]

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
			single.clearArrays(throughput, bandwidth);
			var tpSec = single.initDataArray(throughput, jsonValue, constants.getChartTypes().TP, runMetricsPeriodSec);
			var bwSec = single.initDataArray(bandwidth, jsonValue, constants.getChartTypes().BW, runMetricsPeriodSec);
			console.log(tpSec + " " + bwSec);
			chartsArray.push(common.getScenarioChartObject(runId, runScenarioName,
				[single.drawThroughputCharts(throughput, jsonValue[0], tpSec),
					single.drawBandwidthCharts(bandwidth, jsonValue[0], bwSec)]));
		} else {
			//
			chartsArray.push(common.getScenarioChartObject(runId, runScenarioName,
				[single.drawThroughputCharts(throughput, jsonValue),
					single.drawBandwidthCharts(bandwidth, jsonValue)]));
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
		//
		var throughput = $.extend(true, [], data),
			bandwidth = $.extend(true, [], data);
		//
		if ((array !== undefined) && (array.length > 0)) {
			chain.clearArrays(throughput, bandwidth, runMetricsPeriodSec);
			//
			chain.initDataArray(throughput, array, constants.getChartTypes().TP, runMetricsPeriodSec);
			chain.initDataArray(throughput, array, constants.getChartTypes().BW, runMetricsPeriodSec);
		}
		//
		chartsArray.push({
			"run.id": runId,
			"run.scenario.name": "chain",
			"charts": [
				chain.drawThroughputChart(throughput, runId, runMetricsPeriodSec),
				chain.drawBandwidthChart(bandwidth, runId, runMetricsPeriodSec)
			]
		});
	}

	function drawRampupCharts(chartsArray) {

	}

	return {
		charts: charts
	};

});