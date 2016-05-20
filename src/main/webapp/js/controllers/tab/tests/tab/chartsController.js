define([
	'jquery',
	'../../../../common/util/handlebarsUtil',
	'../../../../common/util/templatesUtil',
	'../../../../common/util/cssUtil',
	'../../../../common/util/tabsUtil',
	'../../../../common/constants',
	'text!../../../../../templates/tab/tests/tab/charts.hbs'
], function ($,
             hbUtil,
             templatesUtil,
             cssUtil,
             tabsUtil,
             constants,
             chartsTemplate) {

	const TAB_TYPE = templatesUtil.tabTypes();
	const TESTS_TAB_TYPE = templatesUtil.testsTabTypes();
	const TESTS_CHARTS_TAB_TYPE = templatesUtil.testsChartsTabTypes();
	const CHART_METRICS = constants.CHART_METRICS;
	const CHART_METRICS_FORMATTER = constants.CHART_METRICS_FORMATTER;
	const plainId = templatesUtil.composeId;
	const jqId = templatesUtil.composeJqId;

	var currentTabType = TESTS_CHARTS_TAB_TYPE.LATENCY;
	var resetChartsFlag = false;

	function render() {
		const renderer = rendererFactory();
		renderer.base();
		makeTabActive(currentTabType);
	}

	const rendererFactory = function () {
		const binder = clickEventBinderFactory();
		const chartsBlockElemId = jqId([TAB_TYPE.TESTS, 'tab', TESTS_TAB_TYPE.CHARTS]);


		function renderBase() {
			hbUtil.compileAndInsertInsideBefore(chartsBlockElemId, chartsTemplate,
				{tabs: TESTS_CHARTS_TAB_TYPE});
			binder.tab();
		}


		return {
			base: renderBase
		}
	};

	const clickEventBinderFactory = function () {

		function bindTabClickEvents() {
			tabsUtil.bindTabClickEvents(TESTS_CHARTS_TAB_TYPE, tabJqId, makeTabActive);
		}

		return {
			tab: bindTabClickEvents
		}
	};

	function tabJqId(tabType) {
		return jqId([tabType, TAB_TYPE.TESTS,  TESTS_TAB_TYPE.CHARTS, 'tab']);
	}

	function makeTabActive(tabType) {
		tabsUtil.showTabAsActive(plainId([TAB_TYPE.TESTS,  TESTS_TAB_TYPE.CHARTS, 'tab']), tabType);
		switch (tabType) {
			case TESTS_CHARTS_TAB_TYPE.LATENCY:
				break;
			case TESTS_CHARTS_TAB_TYPE.DURATION:
				break;
			case TESTS_CHARTS_TAB_TYPE.BANDWIDTH:
				break;
			case TESTS_CHARTS_TAB_TYPE.THROUGHPUT:
				break;
		}
		currentTabType = tabType;
	}

	function updateCharts(metricName, chartsObj) {
		const chartBlock = $(jqId([CHART_METRICS_FORMATTER[metricName], 'chart', 'block']));
	}

	function setTabParameters(testId, testMode) {
		if (LOGS_MODE.indexOf(testMode) > -1) {
			if (testId) {
				resetLogsFlag = false;
				$.each(LOG_MARKER, function (key, value) {
					getLogs(value, testId);
				})
			}
		}
	}

	function getCharts(metricName, testId) {
		$.get('/charts',
			{
				runId: testId
			}
		).done(function (chartsObj) {
			updateLogTable(metricName, chartsObj);
		}).always(function () {
			if (!resetLogsFlag) {
				setTimeout(getCharts, 10000, metricName, testId); // interval in milliseconds;
				// todo check a third arg
			}
		});
	}

	function resetLogs() {
		resetChartsFlag = true;
		resetLogTimeStamps();
		$("." + plainId([TESTS_TAB_TYPE.LOGS, 'table', 'body'])).empty();
	}

	return {
		render: render,
		setTabParameters: setTabParameters,
		resetCharts: resetCharts
	}
});