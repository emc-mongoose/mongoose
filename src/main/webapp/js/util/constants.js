define(function() {

	//  scales constants
	var scaleTypes = ["Log Scale", "Linear Scale"],
		scaleOrientation = ["x", "y"];
	var scales = [
		{
			id: "x",
			types: scaleTypes
		}, {
			id: "y",
			types: scaleTypes
		}
	];

	//  for time accomodation. For more info see #JIRA-314
	var timeLimit = {
		"seconds": {
			"limit": 300,
			"value": 1,
			"next": "minutes",
			"label": "t[seconds]"
		},
		"minutes": {
			"limit": 300,
			"value": 60,
			"next": "hours",
			"label": "t[minutes]"
		},
		"hours": {
			"limit": 120,
			"value": 60 * 60,
			"next": "days",
			"label": "t[hours]"
		},
		"days": {
			"limit": 35,
			"value": 24 * 60 * 60,
			"next": "weeks",
			"label": "t[days]"
		},
		"weeks": {
			"limit": 20,
			"value": 7 * 24 * 60 * 60,
			"next": "months",
			"label": "t[weeks]"
		},
		"months": {
			"limit": 60,
			"value": 4 * 7 * 24 * 60 * 60,
			"next": "years",
			"label": "t[months]"
		},
		"years": {
			"next": null,
			"label": "t[years]"
		}
	};

	//  cfg constants
	var cfgConstants = {
		runId: "run.id",
		runMetricsPeriodSec: "load.metrics.period",
		runScenarioName: "scenario.name"
	};

	//  scenario constants
	var scenario = {
		single: "single",
		chain: "chain",
		rampup: "rampup"
	};

	//  metrics
	var avg = {
			id: "avg",
			text: "total average"
		},
		last = {
			id: "last",
			text: "last 10 sec"
		},
		min = {
			id: "min",
			text: "min"
		},
		max = {
			id: "max",
			text: "max"
		};
	//  max count of points on one chart
	var chartPointsLimit = 1000;
	//
	var chartTypes = {
		TP: "throughput",
		BW: "bandwidth",
		LAT: "latency",
		DUR: "duration"
	};
	//
	var predefinedColors = [
		// primary
		"#0000ff", // b
		"#007f00", // g
		"#c00000", // r
		// secondary
		"#00c0c0", // c
		"#c000c0", // m
		"#c0c000", // y
		// tertiary
		"#00c07f", // spring green
		"#7fc000", // chartreuse
		"#c07f00", // orange
		"#c0007f", // rose
		"#7f00c0", // violet
		"#007fc0" // azure
	];

	return {
		getScaleTypes: function() {
			return scaleTypes;
		},
		getScaleOrientations: function() {
			return scaleOrientation;
		},
		getScales: function() {
			return scales;
		},
		getTimeLimit: function() {
			return timeLimit;
		},
		getCfgConstants: function() {
			return cfgConstants;
		},
		getAvgConstant: function() {
			return avg;
		},
		getLastConstant: function() {
			return last;
		},
		getMinConstant: function() {
			return min;
		},
		getMaxConstant: function() {
			return max;
		},
		getPointsOnChartLimit: function() {
			return chartPointsLimit;
		},
		getChartColors: function() {
			return predefinedColors;
		},
		getScenarioConstant: function() {
			return scenario;
		},
		getChartTypes: function() {
			return chartTypes;
		}
	};
});