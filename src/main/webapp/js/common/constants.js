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

	function metricFormatter(metricName) {
		const formattedName = CHART_METRICS_FORMATTER[metricName];
		if (formattedName) {
			return formattedName;
		} else {
			return null;
		}
	}
	



	return {
		JSON_CONTENT_TYPE: JSON_CONTENT_TYPE,
		LOG_MARKERS: LOG_MARKERS,
		LOG_MARKERS_FORMATTER: LOG_MARKERS_FORMATTER,
		CHART_METRICS: CHART_METRICS,
		metricFormatter: metricFormatter
	}

});