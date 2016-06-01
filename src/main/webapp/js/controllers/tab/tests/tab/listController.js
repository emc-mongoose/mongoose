define([
	'jquery',
	'../../../../common/util/handlebarsUtil',
	'../../../../common/util/templatesUtil',
	'../../../../common/util/cssUtil',
	'../../../../common/util/tabsUtil',
	'../../../../common/constants',
	'./logsController',
	'text!../../../../../templates/tab/tests/tab/list.hbs'
], function ($,
             hbUtil,
             templatesUtil,
             cssUtil,
             tabsUtil,
             constants,
             logsController,
             listTemplate) {

	const TAB_TYPE = templatesUtil.tabTypes();
	const TESTS_TAB_TYPE = templatesUtil.testsTabTypes();
	const plainId = templatesUtil.composeId;
	const jqId = templatesUtil.composeJqId;
	const listItemElemClass = 'list-group-item';

	var currentTestId;
	var currentTestMode;

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
					class: listItemElemClass,
					mode: runMode
				});
			listItemElem.text(runId + " - " + runMode);
			listItemElem.click(function () {
				makeItemActive(runId, runMode)
			});
			const removeIconElem = createRemoveIcon(runId);
			listItemElem.append(removeIconElem);
			testsListBlockElem.append(listItemElem);
		});
		const testsIds = Object.keys(testsObj);
		const lastId = testsIds[testsIds.length - 1];
		makeItemActive(lastId, testsObj[lastId]);
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

	function makeItemActive(testId, testMode) {
		tabsUtil.showTabAsActive(listItemElemClass, testId);
		currentTestId = testId;
		currentTestMode = testMode;
		logsController.resetLogs();
	}

	function getCurrentTestId() {
		return currentTestId;
	}

	function getCurrentTestMode() {
		return currentTestMode;
	}
	
	return {
		render: render,
		updateTestsList: updateTestsList,
		currentTestId: getCurrentTestId,
		currentTestMode: getCurrentTestMode
	}
});