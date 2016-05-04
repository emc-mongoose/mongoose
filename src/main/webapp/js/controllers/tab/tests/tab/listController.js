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
	const listItemElemClass = 'list-group-item';

	var currentTestId;

	function render() {
		const renderer = rendererFactory();
		renderer.base();
	}

	const rendererFactory = function () {
		const listBlockElemId = jqId([TAB_TYPE.TESTS, 'tab', TESTS_TAB_TYPE.LIST]);

		function renderBase() {
			hbUtil.compileAndInsertInsideBefore(listBlockElemId, listTemplate);
		}

		return {
			base: renderBase
		}
	};

	function updateTestsList(testsObj) {
		const testsListBlockElem = $(jqId([TAB_TYPE.TESTS, TESTS_TAB_TYPE.LIST]));
		testsListBlockElem.empty();
		$.each(testsObj, function (key, value) {
			const listItemElem = $('<a/>',
				{
					id: key,
					class: listItemElemClass
				});
			listItemElem.text(key + " - " + value);
			listItemElem.click(function () {
				makeItemActive(key)
			});
			testsListBlockElem.append(listItemElem);
		});
		const testsIds = Object.keys(testsObj);
		makeItemActive(testsIds[testsIds.length - 1]);
	}

	function makeItemActive(testId) {
		tabsUtil.showTabAsActive(listItemElemClass, testId);
		currentTestId = testId;
	}

	return {
		render: render,
		updateTestsList: updateTestsList
	}
});