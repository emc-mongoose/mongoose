define([
	'jquery',
	'../../../../common/util/handlebarsUtil',
	'../../../../common/util/templatesUtil',
	'../../../../common/util/cssUtil',
	'../../../../common/util/tabsUtil',
	'../../../../common/constants',
	'./logsController',
	'./chartsController',
	'text!../../../../../templates/tab/tests/tab/list.hbs'
], function ($,
             hbUtil,
             templatesUtil,
             cssUtil,
             tabsUtil,
             constants,
             logsController,
             chartsController,
             listTemplate) {

	const TAB_TYPE = templatesUtil.tabTypes();
	const TESTS_TAB_TYPE = templatesUtil.testsTabTypes();
	const plainId = templatesUtil.composeId;
	const jqId = templatesUtil.composeJqId;
	const listItemElemClass = 'list-group-item';


	var currentTestId;
	var currentTestMode;
	var okIcon;

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
		$.each(testsObj, function (runId, runIdInfo) {
			const runMode = runIdInfo.mode;
			const runStatus = runIdInfo.status.toLowerCase();
			var listItemElem = $(jqId([runId.replaceAll('\\.', '\\\.')]));
			const listItemElemText = runId + " - " + runMode + " - " + runStatus;
			if (!doesItemExist(listItemElem)) {
				listItemElem = $('<a/>',
					{
						id: runId,
						class: listItemElemClass,
						mode: runMode,
						status: runStatus
					});
				listItemElem.click(function () {
					makeItemActive(runId, runMode)
				});
				testsListBlockElem.append(listItemElem);
			}
			listItemElem.text(listItemElemText);
			const stopIconElem = createStopIcon(runId);
			listItemElem.append(stopIconElem);
			if(!listItemElem.hasClass(runStatus)){
				listItemElem.addClass(runStatus);
			}
		});
		const testsIds = Object.keys(testsObj);
		const lastId = testsIds[testsIds.length - 1];
		makeItemActive(lastId, testsObj[lastId]);
	}

	function doesItemExist(itemElem) {
		return itemElem.length;
	}
	
	function runIdForElem(runId) {
		return runId.replaceAll('\\.', '\\\.');
	}
	
	function createStopIcon(runId) {
		const div = $('<div/>', {
			id: plainId([runId, 'stop']),
			class: 'icon-stop tooltip'
		});
		const tooltipSpan = $('<span/>', {
			class: 'tooltiptext'
		});
		tooltipSpan.text('Click to stop the test');
		div.append(tooltipSpan);
		div.click(function () {
			$(this).off();
			const listItemElem = $(jqId([runIdForElem(runId)]));
			listItemElem.attr('class', listItemElem.attr('class') + ' stopped');
			listItemElem.attr('status', 'stopped');
			listItemElem.text(runId + " - " + (listItemElem.attr('mode')) + " - " + (listItemElem.attr('status')));
			$.ajax({
				type: 'POST',
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

	function createOkIcon(runId) {
		return $('<div/>', {
			id: plainId([runId, 'check']),
			class: 'icon-check'
		});
	}

	function makeItemActive(testId, testMode) {
		tabsUtil.showTabAsActive(listItemElemClass, testId);
		if (okIcon) {
			okIcon.remove();
		}
		okIcon = createOkIcon();
		const listItemActiveElem = $('.' + listItemElemClass + '.active');
		listItemActiveElem.append(okIcon);
		$('.' + listItemElemClass).css('padding-left', '');
		listItemActiveElem.css('padding-left', '40px');
		currentTestId = testId;
		currentTestMode = testMode;
		logsController.resetLogs();
		chartsController.runCharts(testId);
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