define([
	'jquery',
	'../../../../common/util/handlebarsUtil',
	'../../../../common/util/templatesUtil',
	'../../../../common/util/cssUtil',
	'../../../../common/util/tabsUtil',
	'../../../../common/constants',
	'text!../../../../../templates/tab/tests/tab/list.hbs'
], function ($,
             hbUtil,
             templatesUtil,
             cssUtil,
             tabsUtil,
             constants,
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
		$.each(testsObj, function (runId, runMode) {
			const listItemElem = $('<a/>',
				{
					id: runId,
					class: listItemElemClass
				});
			listItemElem.text(runId + " - " + runMode);
			listItemElem.click(function () {
				makeItemActive(runId)
			});
			const removeIconElem = createRemoveIcon(runId);
			listItemElem.append(removeIconElem);
			testsListBlockElem.append(listItemElem);
		});
		const testsIds = Object.keys(testsObj);
		makeItemActive(testsIds[testsIds.length - 1]);
	}

	function createRemoveIcon(runId) {
		const div = $('<div/>', {
			id: plainId([runId, 'remove']),
			class: 'icon-remove'
		});
		div.click(function () {
			$.ajax({
				type: 'DELETE',
				url: '/run',
				dataType: 'json',
				contentType: constants.JSON_CONTENT_TYPE,
				data: JSON.stringify({ runId: runId }),
				processData: false
			}).done(function (testsObj) {
				updateTestsList(testsObj);
				console.log('Mongoose ran');
			});
		});
		return div;
	}

	function makeItemActive(testId) {
		tabsUtil.showTabAsActive(listItemElemClass, testId);
		currentTestId = testId;
	}

	function getCurrentTestId() {
		return currentTestId;
	}

	return {
		render: render,
		updateTestsList: updateTestsList,
		currentTestId: getCurrentTestId
	}
});