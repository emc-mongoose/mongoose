/**
 * Created on 18.04.16.
 */
define([
	'jquery',
	'../../../common/util/handlebarsUtil',
	'../../../common/util/templatesUtil',
	'../../../common/util/cssUtil',
	'../../../common/util/tabsUtil',
	'text!../../../../templates/tab/tests/navbar.hbs',
	'text!../../../../templates/tab/tests/base.hbs',
	'./tab/listController',
	'./tab/logsController',
	'./tab/chartsController'
], function ($,
             hbUtil,
             templatesUtil,
             cssUtil,
             tabsUtil,
             navbarTemplate,
             baseTemplate,
             listController,
             logsController,
             chartsController) {

	const TAB_TYPE = templatesUtil.tabTypes();
	const TESTS_TAB_TYPE = templatesUtil.testsTabTypes();
	const plainId = templatesUtil.composeId;
	const jqId = templatesUtil.composeJqId;
	const CHART_TYPE = templatesUtil.chartTypes();

	var currentTabType = TESTS_TAB_TYPE.LIST;
	var currentChartType = CHART_TYPE.CURRENT;

	function render() {
		const renderer = rendererFactory();
		renderer.navbar();
		renderer.base();
		listController.render();
		logsController.render();
		chartsController.render();
		makeTabActive(currentTabType);
		makeChartTypeActive(currentChartType);
		startPoll();
	}

	function createCaret() {
		return $('<span/>', {
			class: 'caret'
		});
	}

	const rendererFactory = function () {
		const binder = clickEventBinderFactory();
		const testsBlockElemId = jqId([TAB_TYPE.TESTS, 'block']);

		function renderNavbar() {
			hbUtil.compileAndInsertInsideBefore(testsBlockElemId, navbarTemplate,
				{tabs: TESTS_TAB_TYPE});
			const $chartsTab = $(jqId([TESTS_TAB_TYPE.CHARTS, TAB_TYPE.TESTS, 'tab']));
			$chartsTab.addClass('dropdown');
			$chartsTab.empty();
			const $chartsTabA = $('<a/>', {
				id: plainId(['chart', 'type', 'main']),
				class: 'dropdown-toggle',
				'data-toggle': 'dropdown'
			});
			$chartsTabA.text(TESTS_TAB_TYPE.CHARTS + ' ');
			$chartsTabA.append(createCaret());
			$chartsTab.append($chartsTabA);
			$chartsTabA.css('cursor', 'pointer');
			const $chartTypes = $('<ul/>', {
				class: 'dropdown-menu'
			});
			$chartTypes.append(createChartTypeElem(CHART_TYPE.CURRENT));
			$chartTypes.append(createChartTypeElem(CHART_TYPE.TOTAL));
			$chartsTab.append($chartTypes);
			binder.tab();
		}

		function createChartTypeElem(chartType) {
			const $a = $('<a/>', {
				id: plainId(['chart', 'type', chartType])
			});
			$a.text(chartType);
			$a.click(function () {
				makeChartTypeActive(chartType);
			});
			const $li = $('<li/>');
			$li.append($a);
			return $li;
		}

		function renderBase() {
			hbUtil.compileAndInsertInside(testsBlockElemId, baseTemplate);
		}

		return {
			navbar: renderNavbar,
			base: renderBase
		}
	};

	const clickEventBinderFactory = function () {

		function bindTabClickEvents() {
			tabsUtil.bindTabClickEvents(TESTS_TAB_TYPE, tabJqId, makeTabActive);
		}

		return {
			tab: bindTabClickEvents
		}
	};

	function tabJqId(tabType) {
		return jqId([tabType, TAB_TYPE.TESTS, 'tab']);
	}

	function makeChartTypeActive(chartType) {
		const TAB_CLASS = templatesUtil.tabClasses();
		$(jqId(['chart', 'type', currentChartType])).removeClass(TAB_CLASS.ACTIVE);
		$(jqId(['chart', 'type', chartType])).addClass(TAB_CLASS.ACTIVE);
		const $chartType = $(jqId(['chart', 'type', 'main']));
		$chartType.text('Charts: ' + chartType + ' ');
		$chartType.append(createCaret());
		currentChartType = chartType;
		chartsController.setChartType(chartType);
	}

	function makeTabActive(tabType) {
		tabsUtil.showTabAsActive(plainId([TAB_TYPE.TESTS, 'tab']), tabType);
		tabsUtil.showActiveTabDependentElements(plainId([TAB_TYPE.TESTS, 'tab', 'dependent']), tabType);
		const testId = listController.currentTestId();
		const testMode = listController.currentTestMode();
		switch (tabType) {
			case TESTS_TAB_TYPE.LIST:
				break;
			case TESTS_TAB_TYPE.LOGS:
				logsController.setTabParameters(testId, testMode);
				break;
			case TESTS_TAB_TYPE.CHARTS:
				break;
		}
		currentTabType = tabType;
	}

	function updateTestsList(testsObj) {
		listController.updateTestsList(testsObj, true);
	}

	function startPoll() {
		$.ajax({
			type: 'GET',
			url: '/run'
		}).done(function (testsObj) {
			listController.updateTestsList(testsObj, true);
		}).always(pollToUpdateTestList)
	}

	function pollToUpdateTestList() {
		$.ajax({
			type: 'GET',
			url: '/run'
		}).done(function (testsObj) {
			listController.updateTestsList(testsObj, false);
		}).always(function () {
			setTimeout(pollToUpdateTestList, 5000);
		});
	}

	function runCharts() {
		chartsController.runCharts(listController.currentTestId());
	}

	return {
		render: render,
		updateTestsList: updateTestsList,
		runCharts: runCharts
	}
});
