/**
 * Created on 18.04.16.
 */
define([
	'jquery',
	'../../common/util/templatesUtil',
	'../../common/elementAppender',
	'../../common/openFileHandler',
	'../../common/eventCreator',
	'../../common/util/filesUtil'
], function ($,
             templatesUtil,
             elementAppender,
             openFileHandler, 
             eventCreator,
             filesUtil
) {

	const TAB_TYPE = templatesUtil.tabTypes();
	const BLOCK = templatesUtil.blocks();
	const DELIMITER = templatesUtil.delimiters();
	const plainId = templatesUtil.composeId;
	const jqId = templatesUtil.composeJqId;
	const commonClickEventCreator = eventCreator.newClickEventCreator();

	var pureConfigObject;
	var changedConfigObject;

	function setConfigObject(configObj) {
		if (configObj !== null) {
			pureConfigObject = configObj;
			changedConfigObject = $.extend(true, {}, configObj);
		} else {
			pureConfigObject = null;
			changedConfigObject = null;
		}
		filesUtil.changeFileToSaveAs(TAB_TYPE.DEFAULTS, changedConfigObject);
	}

	function setRunMode(currentMode) {
		const tempObj = {run: {mode: currentMode}};
		$.extend(true, changedConfigObject, tempObj);
		filesUtil.changeFileToSaveAs(TAB_TYPE.DEFAULTS, changedConfigObject);
	}

	function render(configObject) {
		setConfigObject(configObject);
		const rootTreeUlElem = $(jqId([BLOCK.TREE, TAB_TYPE.DEFAULTS]));
		rootTreeUlElem.empty();
		var addressObject = {};
		elementAppender.objectAsTree(configObject, rootTreeUlElem, 'prop', addressObject, DELIMITER.PROPERTY, '', commonClickEventCreator.propertyClickEvent);
		const treeFormElem = $(jqId([BLOCK.CONFIG, 'form', TAB_TYPE.DEFAULTS]));
		treeFormElem.empty();
		elementAppender.formForTree(addressObject, treeFormElem, DELIMITER.PROPERTY, changedConfigObject, TAB_TYPE.DEFAULTS);
	}


	function fileReaderOnLoadAction(configObject, fullFileName) {
		render(configObject);
		// $(jqId(['file', 'name', TAB_TYPE.DEFAULTS])).val(fullFileName);
	}


	function setTabParameters() {
		openFileHandler.setFileReaderOnLoadAction(fileReaderOnLoadAction);
	}

	function getPureAppConfig() {
		return pureConfigObject;
	}

	function getChangedAppConfig() {
		return changedConfigObject;
	}
	
	return {
		render: render,
		setTabParameters: setTabParameters,
		getPureAppConfig: getPureAppConfig,
		getChangedAppConfig: getChangedAppConfig,
		setRunMode: setRunMode
	}
});
