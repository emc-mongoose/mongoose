/**
 * Created on 18.04.16.
 */
define([
	'jquery',
	'../../common/util/templatesUtil',
	'../../common/util/cssUtil',
	'../../common/elementAppender',
	'../../common/openFileHandler',
	'../../common/eventCreator',
	'../../common/util/filesUtil'
], function ($,
             templatesUtil,
             cssUtil,
             elementAppender,
             openFileHandler, 
             eventCreator,
             filesUtil
) {
	const TAB_TYPE = templatesUtil.tabTypes();
	const BLOCK = templatesUtil.blocks();
	const TREE_ELEM = templatesUtil.configTreeElements();
	const DELIMITER = templatesUtil.delimiters();
	const plainId = templatesUtil.composeId;
	const jqId = templatesUtil.composeJqId;
	var mainViewFlag = true;
	var pureScenarioObject = null;
	var changedScenarioObject = null;

	function setScenarioObject(scenarioObj) {
		if (scenarioObj !== null) {
			pureScenarioObject = scenarioObj;
			changedScenarioObject = $.extend(true, {}, scenarioObj);
		} else {
			pureScenarioObject = null;
			changedScenarioObject = null;
		}
		filesUtil.changeFileToSave(TAB_TYPE.SCENARIOS, changedScenarioObject);
	}

	const clickEventCreatorFactory = function () {
		
		function scenarioFileClickEvent(aName) {
			$.get('/scenario', {path: aName}, null, 'json')
				.done(function (scenarioJson) {
					setScenarioObject(scenarioJson);
					updateDetailsTree(scenarioJson);
					$(jqId(['file', 'name', TAB_TYPE.SCENARIOS])).val(aName);
				})
				.fail(function () {
					alert('The scenario cannot be loaded')
				})
		}
		
		function backClickEvent() {
			showMainTree();
			$(jqId(['file', 'name', TAB_TYPE.SCENARIOS])).val('No scenario chosen');
		}

		return {
			backToUpperLevel: backClickEvent,
			scenarioFile: scenarioFileClickEvent
		}
	};

	const localClickEventCreator = clickEventCreatorFactory();
	const commonClickEventCreator = eventCreator.newClickEventCreator();

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
		const jsonViewElem = $(jqId(['json', TAB_TYPE.SCENARIOS]));
		jsonViewElem.text(JSON.stringify(scenarioObject, null, 4));
		showDetailsTree();
		const treeFormElem = $(jqId([BLOCK.CONFIG, 'form', TAB_TYPE.SCENARIOS]));
		treeFormElem.empty();
		elementAppender.formForTree(addressObject, treeFormElem, DELIMITER.PROPERTY, changedScenarioObject, TAB_TYPE.SCENARIOS, jsonViewElem);
	}

	function showMainTree() {
		$(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS, 'details'])).hide();
		$(jqId(['json', TAB_TYPE.SCENARIOS])).hide();
		$(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS])).show();
		mainViewFlag = true;
	}

	function showDetailsTree() {
		$(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS, 'details'])).show();
		$(jqId(['json', TAB_TYPE.SCENARIOS])).show();
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
			setScenarioObject(null);
		});
		return div;
	}


	function fileReaderOnLoadAction(scenarioObject, fullFileName) {
		setScenarioObject(scenarioObject);
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

	function getPureScenario() {
		return pureScenarioObject;
	}

	function getChangedScenario() {
		return changedScenarioObject;
	}
	
	function isChanged() {
		return !filesUtil.compareObjects(pureScenarioObject, changedScenarioObject);
	}
	
	return {
		render: render,
		setTabParameters: setTabParameters,
		getPureScenario: getPureScenario,
		getChangedScenario: getChangedScenario,
		isChanged: isChanged
	}
});

