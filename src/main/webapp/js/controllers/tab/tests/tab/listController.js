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

	function updateTestsList(testsArr) {
		const testsListBlockElem = $(jqId([TAB_TYPE.TESTS, TESTS_TAB_TYPE.LIST]));
		testsListBlockElem.empty();
		$.each(testsArr, function (index, value) {
			const listItemElem = $('<a/>',
				{
					id: value,
					class: listItemElemClass
				});
			listItemElem.text(value);
			listItemElem.click(function () {
				makeItemActive(value)
			});
			testsListBlockElem.append(listItemElem);
		});
		makeItemActive(testsArr[testsArr.length - 1]);
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