define([
	'jquery',
	'../../../../common/util/handlebarsUtil',
	'../../../../common/util/templatesUtil',
	'../../../../common/util/cssUtil',
	'../../../../common/util/tabsUtil',
	'../../../../common/constants',
	'../../../../charts/main',
	'text!../../../../../templates/tab/tests/tab/charts.hbs'
], function ($,
             hbUtil,
             templatesUtil,
             cssUtil,
             tabsUtil,
             constants,
             charts,
             chartsTemplate) {

	const TAB_TYPE = templatesUtil.tabTypes();
	const TESTS_TAB_TYPE = templatesUtil.testsTabTypes();
	const TESTS_CHARTS_TAB_TYPE = templatesUtil.testsChartsTabTypes();
	const CHART_METRICS = constants.CHART_METRICS;
	const CHART_METRICS_FORMATTER = constants.CHART_METRICS_FORMATTER;
	const CHARTS_MODE = templatesUtil.objPartToArray(templatesUtil.modes(), 2);
	const plainId = templatesUtil.composeId;
	const jqId = templatesUtil.composeJqId;

	var currentTabType = TESTS_CHARTS_TAB_TYPE.DURATION;
	var currentChartType;
	var resetChartsFlags = {};

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
		return jqId([tabType, TAB_TYPE.TESTS, TESTS_TAB_TYPE.CHARTS, 'tab']);
	}

	function makeTabActive(tabType) {
		tabsUtil.showTabAsActive(plainId([TAB_TYPE.TESTS, TESTS_TAB_TYPE.CHARTS, 'tab']), tabType);
		charts.processCharts(null, CHART_METRICS_FORMATTER[tabType], currentChartType, true);
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

	function runCharts(testId) {
		resetCharts(testId);
		if (testId) {
			resetChartsFlags[testId] = false;
			getCharts(testId);
		}
	}
	
	function getCharts(testId) {
		$.get('/charts',
			{
				runId: testId
			}
		).done(function (chartsObj) {
			if (!resetChartsFlags[testId]) {
				charts.processCharts(chartsObj, CHART_METRICS_FORMATTER[currentTabType], currentChartType);
			}
		}).always(function () {
			if (!resetChartsFlags[testId]) {
				setTimeout(getCharts, 10000, testId); // interval in milliseconds;
			}
		});
	}

	function resetCharts() {
		const runIds = Object.keys(resetChartsFlags);
		runIds.forEach(function (runId) {
			resetChartsFlags[runId] = true;
		});
		if (runIds.length > 0) {
			$(jqId([TESTS_TAB_TYPE.CHARTS, 'block'])).empty();
		}
	}

	function setCurrentChartType(chartType) {
		currentChartType = chartType;
		makeTabActive(currentTabType);
	}

	return {
		render: render,
		runCharts: runCharts,
		resetCharts: resetCharts,
		setChartType: setCurrentChartType
	}
});