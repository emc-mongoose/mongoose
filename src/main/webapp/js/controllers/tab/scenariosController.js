/**
 * Created on 18.04.16.
 */
define([
	'jquery',
	'../../util/templatesUtil',
	'../../util/cssUtil',
	'../../common/elementAppender',
	'../../common/openFileHandler',
	'../../common/eventCreator'
], function ($,
             templatesUtil,
             cssUtil,
             elementAppender,
             openFileHandler, 
             eventCreator) {
	const TAB_TYPE = templatesUtil.tabTypes();
	const BLOCK = templatesUtil.blocks();
	const TREE_ELEM = templatesUtil.configTreeElements();
	const DELIMITER = templatesUtil.delimiters();
	const plainId = templatesUtil.composeId;
	const jqId = templatesUtil.composeJqId;
	var mainViewFlag = true;
	var currentScenarioObject = null;

	const clickEventCreatorFactory = function () {
		
		function scenarioFileClickEvent(aHref) {
			$.post('/scenario', {path: aHref})
				.done(function (scenarioJson) {
					currentScenarioObject = scenarioJson;
					updateDetailsTree(scenarioJson);
					$(jqId(['file', 'name', TAB_TYPE.SCENARIOS])).val(aHref);
				})
		}
		
		function backClickEvent() {
			showMainTree();
			// cssUtil.hide('.' + plainId(['form', TAB_TYPE.SCENARIOS, 'property']));
			// $(jqId(['configuration', 'content'])).empty();
			$(jqId(['file', 'name', TAB_TYPE.SCENARIOS])).val('No scenario chosen');
		}

		return {
			backToUpperLevel: backClickEvent,
			scenarioFile: scenarioFileClickEvent
		}
	};

	const localClickEventCreator = clickEventCreatorFactory();
	const commonClickEventCreator = eventCreator.getInstance();

	function render(scenariosArray) {
		const rootTreeUlElem = $(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS]));
		elementAppender.arrayAsTree(scenariosArray, rootTreeUlElem, 'dir', DELIMITER.PATH, localClickEventCreator.scenarioFile);
	}

	function updateDetailsTree(scenarioObject) {
		const treeUlElem = $(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS, 'details']));
		treeUlElem.empty();
		treeUlElem.append(createBackIcon());
		var addressObject = {};
		elementAppender.objectAsTree(scenarioObject, treeUlElem, TREE_ELEM.LEAF, addressObject, DELIMITER.PROPERTY, '', commonClickEventCreator.propertyClickEvent);
		showDetailsTree();
		const treeFormElem = $(jqId([BLOCK.CONFIG, 'form', TAB_TYPE.SCENARIOS]));
		treeFormElem.empty();
		elementAppender.formForTree(addressObject, treeFormElem, DELIMITER.PROPERTY);
	}

	function showMainTree() {
		$(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS, 'details'])).hide();
		$(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS])).show();
		mainViewFlag = true;
	}

	function showDetailsTree() {
		$(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS, 'details'])).show();
		$(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS])).hide();
		mainViewFlag = false;
	}

	function createBackIcon() {
		const div = $('<div/>', {
			id: plainId([TAB_TYPE.SCENARIOS, 'one', 'back', 'id']),
			class: 'icon-reply'
		});
		div.click(function () {
			localClickEventCreator.backToUpperLevel();
			const treeFormElem = $(jqId([BLOCK.CONFIG, 'form', TAB_TYPE.SCENARIOS]));
			treeFormElem.empty();
			currentScenarioObject = null;
		});
		return div;
	}


	function fileReaderOnLoadAction(scenarioObject, fullFileName) {
		currentScenarioObject = scenarioObject;
		updateDetailsTree(scenarioObject);
		// $(jqId(['file', 'name', TAB_TYPE.SCENARIOS])).val(fullFileName);
	}

	function setTabParameters() {
		if (mainViewFlag) {
			showMainTree();
		} else {
			showDetailsTree();
		}
		openFileHandler.setFileReaderOnLoadAction(fileReaderOnLoadAction);
	}

	function getCurrentScenario() {
		return currentScenarioObject;
	}
	
	return {
		render: render,
		setTabParameters: setTabParameters,
		getCurrentScenario: getCurrentScenario
	}
});

