/**
 * Created on 18.04.16.
 */
define([
	'jquery',
	'../../util/templatesUtil',
	'../../common/elementAppender',
	'../../common/openFileHandler'
], function ($,
             templatesUtil,
             elementAppender,
             openFileHandler) {

	const TAB_TYPE = templatesUtil.tabTypes();
	const BLOCK = templatesUtil.blocks();
	const DELIMITER = templatesUtil.delimiters();
	const plainId = templatesUtil.composeId;
	const jqId = templatesUtil.composeJqId;

	var currentConfigObject;

	function render(configObject) {
		currentConfigObject = configObject;
		const rootTreeUlElem = $(jqId([BLOCK.TREE, TAB_TYPE.DEFAULTS]));
		rootTreeUlElem.empty();
		var addressObject = {};
		elementAppender.objectAsTree(configObject, rootTreeUlElem, 'prop', addressObject, DELIMITER.PROPERTY);
	}


	function fileReaderOnLoadAction(configObject, fullFileName) {
		render(configObject);
		// $(jqId(['file', 'name', TAB_TYPE.DEFAULTS])).val(fullFileName);
	}


	function setTabParameters() {
		openFileHandler.setFileReaderOnLoadAction(fileReaderOnLoadAction);
	}

	function getCurrentAppConfiguration() {
		return currentConfigObject;
	}
	
	return {
		render: render,
		setTabParameters: setTabParameters,
		getCurrentAppConfiguration: getCurrentAppConfiguration
	}
});
