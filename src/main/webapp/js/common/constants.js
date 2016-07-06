define([
], function () {

	const JSON_CONTENT_TYPE = 'application/json; charset=utf-8';

	const LOG_MARKERS = {
		'messages': 'msg',
		'errors': 'err',
		'perf\.avg': 'perfAvg',
		'perf\.sum': 'perfSum'
	};

	const LOG_MARKERS_FORMATTER = {
		'msg': 'messages',
		'err': 'errors',
		'perfAvg': 'perf\\.avg',
		'perfSum': 'perf\\.sum',
		'messages': 'msg',
		'errors': 'err',
		'perf\.avg': 'perfAvg',
		'perf\.sum': 'perfSum'
	};

	const CHART_METRICS = {
		'latency': 'lat',
		'duration': 'dur',
		'throughput': 'TP',
		'bandwidth': 'BW'
	};

	const CHART_METRICS_FORMATTER = {
		'lat': 'latency',
		'dur': 'duration',
		'TP': 'throughput',
		'BW': 'bandwidth',
		'latency': 'lat',
		'duration': 'dur',
		'throughput': 'TP',
		'bandwidth': 'BW'
	};

	const sec = 's';
	const byte = 'B';

	function micro(unit) {
		return '\u03bc' + unit;
	}

	function mega(unit) {
		return 'M' + unit;
	}

	function metricWithUnit(metric, unit) {
		return metric + '[' + unit + ']';
	}

	const CHART_METRICS_UNITS_FORMATTER = {
		'lat': metricWithUnit('latency', micro(sec)),
		'dur': metricWithUnit('duration', micro(sec)),
		'TP': metricWithUnit('rate', 'obj/' + sec),
		'BW': metricWithUnit('rate', mega(byte) + '/' + sec)
	};

	return {
		JSON_CONTENT_TYPE: JSON_CONTENT_TYPE,
		LOG_MARKERS: LOG_MARKERS,
		LOG_MARKERS_FORMATTER: LOG_MARKERS_FORMATTER,
		CHART_METRICS: CHART_METRICS,
		CHART_METRICS_FORMATTER: CHART_METRICS_FORMATTER,
		CHART_METRICS_UNITS_FORMATTER: CHART_METRICS_UNITS_FORMATTER
	}

});