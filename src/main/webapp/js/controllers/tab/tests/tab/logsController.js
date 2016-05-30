define([
	'jquery',
	'../../../../common/util/handlebarsUtil',
	'../../../../common/util/templatesUtil',
	'../../../../common/util/cssUtil',
	'../../../../common/util/tabsUtil',
	'../../../../common/constants',
	'text!../../../../../templates/tab/tests/tab/logs.hbs'
], function ($,
             hbUtil,
             templatesUtil,
             cssUtil,
             tabsUtil,
             constants,
             logsTemplate) {

	const TAB_TYPE = templatesUtil.tabTypes();
	const TESTS_TAB_TYPE = templatesUtil.testsTabTypes();
	const TESTS_LOGS_TAB_TYPE = templatesUtil.testsLogsTabTypes();
	const LOG_MARKER = constants.LOG_MARKERS;
	const LOG_MARKER_FORMATTER = constants.LOG_MARKERS_FORMATTER;
	const LOGS_MODE = templatesUtil.objPartToArray(templatesUtil.modes(), 2);
	const plainId = templatesUtil.composeId;
	const jqId = templatesUtil.composeJqId;

	var currentTabType = TESTS_LOGS_TAB_TYPE.MESSAGES;
	var resetLogsFlags = {};
	var currentLogsTimeStamps = {
			'msg': 0,
			'err': 0,
			'perfAvg': 0,
			'perfSum': 0
	};

	function resetLogTimeStamps() {
		$.each(currentLogsTimeStamps, function (key) {
			currentLogsTimeStamps[key] = 0;
		})
	}

	function render() {
		const renderer = rendererFactory();
		renderer.base();
		makeTabActive(currentTabType);
	}

	const rendererFactory = function () {
		const binder = clickEventBinderFactory();
		const logsBlockElemId = jqId([TAB_TYPE.TESTS, 'tab', TESTS_TAB_TYPE.LOGS]);

		function renderBase() {
			hbUtil.compileAndInsertInsideBefore(logsBlockElemId, logsTemplate,
				{tabs: TESTS_LOGS_TAB_TYPE});
			binder.tab();
		}

		return {
			base: renderBase
		}
	};

	const clickEventBinderFactory = function () {

		function bindTabClickEvents() {
			tabsUtil.bindTabClickEvents(TESTS_LOGS_TAB_TYPE, tabJqId, makeTabActive);
		}

		return {
			tab: bindTabClickEvents
		}
	};

	function tabJqId(tabType) {
		if (tabType.indexOf('.') > 0) {
			tabType = tabType.replaceAll('\\.', '\\\.')
		}
		return jqId([tabType, TAB_TYPE.TESTS, TESTS_TAB_TYPE.LOGS, 'tab']);
	}

	function makeTabActive(tabType) {
		tabsUtil.showTabAsActive(plainId([TAB_TYPE.TESTS, TESTS_TAB_TYPE.LOGS, 'tab']), tabType);
		tabsUtil.showActiveTabDependentElements(plainId([TAB_TYPE.TESTS, TESTS_TAB_TYPE.LOGS, 'tab', 'dependent']), tabType);
		switch (tabType) {
			case TESTS_LOGS_TAB_TYPE.MESSAGES:
				break;
			case TESTS_LOGS_TAB_TYPE.ERRORS:
				break;
			case TESTS_LOGS_TAB_TYPE.PERFAVG:
				break;
			case TESTS_LOGS_TAB_TYPE.PERFSUM:
				break;
		}
		currentTabType = tabType;
	}
	
	function updateLogTable(markerName, logsObj) {
		const tableBody = $(jqId([LOG_MARKER_FORMATTER[markerName], 'log', 'wrapper']) + " ." + plainId([TESTS_TAB_TYPE.LOGS, 'table', 'body']));
		$.each(logsObj, function (key, logEvents) {
			$.each(logEvents, function (index, logEvent) {
				const tableRow = $("<tr/>");
				$.each(logEvent, function (key, value) {
					const tableCell = $("<td/>");
					tableCell.text(value);
					tableRow.append(tableCell);
				});
				tableBody.append(tableRow);
			});
			if (logEvents.length > 0) {
				const lastTimeStamp = logEvents[logEvents.length - 1].timeStamp;
				if (lastTimeStamp) {
					
					currentLogsTimeStamps[markerName] = lastTimeStamp;
				}
			}
		});
	}
	
	function getLogs(markerName, testId) {
		$.get('/logs',
				{ 
					runId: testId,
					markerName: markerName,
					timeStamp: currentLogsTimeStamps[markerName]
				}
		).done(function (logsObj) {
			updateLogTable(markerName, logsObj);
		}).always(function () {
			if (!resetLogsFlags[testId]) {
				setTimeout(getLogs, 10000, markerName, testId); // interval in milliseconds; todo check a third arg
			} else {
				$("." + plainId([TESTS_TAB_TYPE.LOGS, 'table', 'body'])).empty();
			}
		});
	}

	function setTabParameters(testId, testMode) {
		if (LOGS_MODE.indexOf(testMode) > -1) {
			if (testId) {
				resetLogsFlags[testId] = false;
				$.each(LOG_MARKER, function (key, value) {
					getLogs(value, testId);
				})
			}
		}
	}
	
	function resetLogs() {
		Object.keys(resetLogsFlags).forEach(function (key) {
			resetLogsFlags[key] = true;
		});
		resetLogTimeStamps();
		$("." + plainId([TESTS_TAB_TYPE.LOGS, 'table', 'body'])).empty();
	}

	return {
		render: render,
		setTabParameters: setTabParameters,
		resetLogs: resetLogs
	}
});