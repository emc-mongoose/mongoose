/**
 * Created on 18.04.16.
 */
define([
	'jquery',
	'../../util/templatesUtil',
	'../../common/elementAppender',
	'../../common/openFileHandler',
	'../../common/eventCreator'
], function ($,
             templatesUtil,
             elementAppender,
             openFileHandler, 
             eventCreator) {

	const TAB_TYPE = templatesUtil.tabTypes();
	const BLOCK = templatesUtil.blocks();
	const DELIMITER = templatesUtil.delimiters();
	const plainId = templatesUtil.composeId;
	const jqId = templatesUtil.composeJqId;
	const commonClickEventCreator = eventCreator.newClickEventCreator();

	var runConfigObject;
	var saveConfigObject;

	function setConfigObject(configObj) {
		if (configObj !== null) {
			runConfigObject = configObj;
			saveConfigObject = $.extend(true, {}, configObj);
		} else {
			runConfigObject = null;
			saveConfigObject = null;
		}
	}

	function render(configObject) {
		setConfigObject(configObject);
		const rootTreeUlElem = $(jqId([BLOCK.TREE, TAB_TYPE.DEFAULTS]));
		rootTreeUlElem.empty();
		var addressObject = {};
		elementAppender.objectAsTree(configObject, rootTreeUlElem, 'prop', addressObject, DELIMITER.PROPERTY, '', commonClickEventCreator.propertyClickEvent);
		const treeFormElem = $(jqId([BLOCK.CONFIG, 'form', TAB_TYPE.DEFAULTS]));
		treeFormElem.empty();
		elementAppender.formForTree(addressObject, treeFormElem, DELIMITER.PROPERTY, saveConfigObject);
	}


	function fileReaderOnLoadAction(configObject, fullFileName) {
		render(configObject);
		// $(jqId(['file', 'name', TAB_TYPE.DEFAULTS])).val(fullFileName);
	}


	function setTabParameters() {
		openFileHandler.setFileReaderOnLoadAction(fileReaderOnLoadAction);
	}

	function getRunAppConfig() {
		return runConfigObject;
	}

	function getSaveAppConfig() {
		return saveConfigObject;
	}
	
	return {
		render: render,
		setTabParameters: setTabParameters,
		getRunAppConfig: getRunAppConfig,
		getSaveAppConfig: getSaveAppConfig
	}
});
