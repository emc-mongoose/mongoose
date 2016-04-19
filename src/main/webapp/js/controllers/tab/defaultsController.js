/**
 * Created on 18.04.16.
 */
define(['jquery', 
	'../../util/templatesUtil', 
	'../../dom/elementAppender'], 
	function (
		$, 
		templatesUtil, 
		elementAppender) {

	const TAB_TYPE = templatesUtil.tabTypes();
	const BLOCK = templatesUtil.blocks();
	const DELIMITER = templatesUtil.delimiters();
	const plainId = templatesUtil.composeId;
	const jqId = templatesUtil.composeJqId;
	
	function render(configObject) {
		const rootTreeUlElem = $(jqId([BLOCK.TREE, TAB_TYPE.DEFAULTS]));
		var addressObject = {};
		elementAppender.objectAsTree(configObject, rootTreeUlElem, 'prop', addressObject, DELIMITER.PROPERTY);
	}
	
	return {
		render: render
	}
});
