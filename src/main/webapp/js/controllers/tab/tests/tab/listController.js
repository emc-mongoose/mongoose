define([
	'jquery',
	'../../../../common/util/handlebarsUtil',
	'../../../../common/util/templatesUtil',
	'../../../../common/util/cssUtil',
	'../../../../common/util/tabsUtil',
	'text!../../../../../templates/tab/tests/tab/list.hbs'
], function ($,
             hbUtil,
             templatesUtil,
             cssUtil,
             tabsUtil,
             listTemplate) {

	const TAB_TYPE = templatesUtil.tabTypes();
	const TESTS_TAB_TYPE = templatesUtil.testsTabTypes();
	const plainId = templatesUtil.composeId;
	const jqId = templatesUtil.composeJqId;

	var currentTabType = TESTS_TAB_TYPE.LIST;

	function render() {
		const renderer = rendererFactory();
		// renderer.navbar();
		// renderer.base();
		// makeTabActive(currentTabType);
	}

	const rendererFactory = function () {
		const binder = clickEventBinderFactory();
		const listBlockElemId = jqId([TAB_TYPE.TESTS, 'tab', TESTS_TAB_TYPE.LIST]);

		function renderNavbar() {
			hbUtil.compileAndInsertInsideBefore(listBlockElemId, listTemplate,
				{tabs: TESTS_TAB_TYPE});
			binder.tab();
		}

		return {
			navbar: renderNavbar
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

	function makeTabActive(tabType) {
		tabsUtil.showTabAsActive(plainId([TAB_TYPE.TESTS, 'tab']), tabType);
		switch (tabType) {
			case TESTS_TAB_TYPE.LIST:
				break;
			case TESTS_TAB_TYPE.LOGS:
				break;
			case TESTS_TAB_TYPE.CHARTS:
				break;
		}
		currentTabType = tabType;
	}

	return {
		render: render
	}
});