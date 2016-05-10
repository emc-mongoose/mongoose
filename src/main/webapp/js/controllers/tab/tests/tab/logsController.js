define([
	'jquery',
	'../../../../common/util/handlebarsUtil',
	'../../../../common/util/templatesUtil',
	'../../../../common/util/cssUtil',
	'../../../../common/util/tabsUtil',
	'../../../../common/constants',
	'./listController',
	'text!../../../../../templates/tab/tests/tab/logs.hbs'
], function ($,
             hbUtil,
             templatesUtil,
             cssUtil,
             tabsUtil,
             constants,
             listController,
             logsTemplate) {

	const TAB_TYPE = templatesUtil.tabTypes();
	const TESTS_TAB_TYPE = templatesUtil.testsTabTypes();
	const TESTS_LOGS_TAB_TYPE = templatesUtil.testsLogsTabTypes();
	const plainId = templatesUtil.composeId;
	const jqId = templatesUtil.composeJqId;

	var currentTabType = TESTS_LOGS_TAB_TYPE.MESSAGES;
	var currentTimeStamp = 0;

	function render() {
		const renderer = rendererFactory();
		renderer.navbar();
		makeTabActive(currentTabType);
	}

	const rendererFactory = function () {
		const binder = clickEventBinderFactory();
		const logsBlockElemId = jqId([TAB_TYPE.TESTS, 'tab', TESTS_TAB_TYPE.LOGS]);

		function renderNavbar() {
			hbUtil.compileAndInsertInsideBefore(logsBlockElemId, logsTemplate,
				{tabs: TESTS_LOGS_TAB_TYPE});
			binder.tab();
		}

		return {
			navbar: renderNavbar
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
		console.log(markerName);
		console.log(logsObj);
	}
	
	function getLogs(markerName) {
		$.ajax({
			type: 'GET',
			url: '/logs',
			dataType: 'json',
			contentType: constants.JSON_CONTENT_TYPE,
			data: JSON.stringify(
				{ 
					runId: listController.currentTestId(),
					markerName: markerName,
					timeStamp: currentTimeStamp
				}),
			processData: false
		}).done(function (logsObj) {
			updateLogTable(markerName, logsObj);
		}).always(function () {
			setTimeout(getLogs, 10000, markerName); // interval in milliseconds; todo check a third arg
		});
	}

	return {
		render: render
	}
});