define([
], function () {

	const JSON_CONTENT_TYPE = 'application/json; charset=utf-8';

	const LOG_MARKERS = {
		'msg': 'messages',
		'err': 'errors',
		'perfAvg': 'perf\.avg',
		'perfSum': 'perf\.sum',
		'messages': 'msg',
		'errors': 'err',
		'perf\.avg': 'perfAvg',
		'perf\.sum': 'perfSum'
	};

	return {
		JSON_CONTENT_TYPE: JSON_CONTENT_TYPE,
		LOG_MARKERS: LOG_MARKERS
	}

});