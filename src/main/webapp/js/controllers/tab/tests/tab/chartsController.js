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
	const svgId = 'chartboard';

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
		return jqId([tabType, TAB_TYPE.TESTS, TESTS_TAB_TYPE.CHARTS, 'tab']);
	}

	function makeTabActive(tabType) {
		tabsUtil.showTabAsActive(plainId([TAB_TYPE.TESTS, TESTS_TAB_TYPE.CHARTS, 'tab']), tabType);
		// tabsUtil.showActiveTabDependentElements(plainId([TAB_TYPE.TESTS, TESTS_TAB_TYPE.CHARTS, 'tab', 'dependent']), tabType);
		switch (tabType) {
			case TESTS_CHARTS_TAB_TYPE.LATENCY:
				charts.updateChartBoardView(jqId([svgId]), CHART_METRICS_FORMATTER[TESTS_CHARTS_TAB_TYPE.LATENCY]);
				break;
			case TESTS_CHARTS_TAB_TYPE.DURATION:
				charts.updateChartBoardView(jqId([svgId]), CHART_METRICS_FORMATTER[TESTS_CHARTS_TAB_TYPE.DURATION]);
				break;
			case TESTS_CHARTS_TAB_TYPE.BANDWIDTH:
				charts.updateChartBoardView(jqId([svgId]), CHART_METRICS_FORMATTER[TESTS_CHARTS_TAB_TYPE.BANDWIDTH]);
				break;
			case TESTS_CHARTS_TAB_TYPE.THROUGHPUT:
				charts.updateChartBoardView(jqId([svgId]), CHART_METRICS_FORMATTER[TESTS_CHARTS_TAB_TYPE.THROUGHPUT]);
				break;
		}
		currentTabType = tabType;
	}

	function setTabParameters(testId, testMode) {
		resetCharts();
		if (CHARTS_MODE.indexOf(testMode) > -1) {
			if (testId) {
				resetChartsFlag = false;
				getCharts(testId);
			}
		}
	}

	function getCharts(testId) {
		$.get('/charts',
			{
				runId: testId
			}
		).done(function (chartsObj) {
			$.each(chartsObj, function (loadJobName, chartsByLoadJob) {
				const svgBlockId = plainId([TESTS_TAB_TYPE.CHARTS, 'block']);
				if (!$(jqId([svgId])).length) {
					charts.createChartBoard(jqId([svgBlockId]), svgId, loadJobName, chartsByLoadJob);
				}
				charts.updateChartBoardContent(jqId([svgId]), loadJobName, chartsByLoadJob, CHART_METRICS_FORMATTER[currentTabType]);
			});
		}).always(function () {
			if (!resetChartsFlag) {
				setTimeout(getCharts, 10000, testId); // interval in milliseconds;
				// todo check a third arg
			}
		});
	}

	function resetCharts() {
		resetChartsFlag = true;
		// $.each(CHART_METRICS, function (key, metricName) {
		// 	$(jqId([metricBlockId(metricName)])).empty();
		// })
	}

	return {
		render: render,
		setTabParameters: setTabParameters,
		resetCharts: resetCharts
	}
});