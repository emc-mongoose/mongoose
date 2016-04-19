/**
 * Created on 18.04.16.
 */
/**
 * Created on 18.04.16.
 */
define([
	'jquery',
	'../../util/templatesUtil',
	'../../util/cssUtil',
	'../../dom/elementAppender',
	'../../dom/elementCreator'
], function ($,
             templatesUtil,
             cssUtil,
             elementAppender, 
             elementCreator) {

	const TAB_TYPE = templatesUtil.tabTypes();
	const BLOCK = templatesUtil.blocks();
	const TREE_ELEM = templatesUtil.configTreeElements();
	const DELIMITER = templatesUtil.delimiters();
	const plainId = templatesUtil.composeId;
	const jqId = templatesUtil.composeJqId;
	var currentScenarioJson = {};
	
	const clickEventCreatorFactory = function () {
		var prevPropInputId = '';
		function scenarioFileClickEvent(aHref) {
			$.post('/scenario', {path: aHref})
				.done(function (scenarioJson) {
					currentScenarioJson = scenarioJson;
					updateDetailsTree(scenarioJson);
					$(jqId(['config', 'file', 'name', TAB_TYPE.SCENARIOS])).val(aHref);
				})
		}
		function scenarioPropertyClickEvent(aHref) {
			aHref = aHref.replaceAll('\\.', '\\\.');
			const currentPropInputId = jqId([aHref]);
			if (currentPropInputId !== prevPropInputId) {
				cssUtil.hide(prevPropInputId);
				cssUtil.show(currentPropInputId);
				prevPropInputId = currentPropInputId;
			}
		}
		function backClickEvent() {
			showMainTree();
			cssUtil.hide('.' + plainId(['form', TAB_TYPE.SCENARIOS, 'property']));
			$(jqId(['configuration', 'content'])).empty();
			$(jqId(['config', 'file', 'name', TAB_TYPE.SCENARIOS])).val('No scenario chosen');
		}
		return {
			backToUpperLevel: backClickEvent,
			scenarioFile: scenarioFileClickEvent,
			scenarioProperty: scenarioPropertyClickEvent
		}
	};
	
	const clickEventCreator = clickEventCreatorFactory();

	function render(scenariosArray) {
		const rootTreeUlElem = $(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS]));
		elementAppender.arrayAsTree(scenariosArray, rootTreeUlElem, 'dir', DELIMITER.PATH, clickEventCreator.scenarioFile);
	}

	function updateDetailsTree(scenarioJson) {
		const treeUlElem = $(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS, 'details']));
		treeUlElem.empty();
		treeUlElem.append(createBackIcon());
		var addressObject = {};
		elementAppender.objectAsTree(scenarioJson, treeUlElem, TREE_ELEM.LEAF, addressObject, DELIMITER.PROPERTY, '', clickEventCreator.scenarioProperty);
		showDetailsTree();
		// $(jqId(['configuration', 'content'])).append(elementCreator.treeFormElem((addressObject, BLOCK.TREE, DELIMITER.PROPERTY)));
	}

	function showMainTree() {
		$(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS, 'details'])).hide();
		$(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS])).show();
	}

	function showDetailsTree() {
		$(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS, 'details'])).show();
		$(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS])).hide();
	}

	function createBackIcon() {
		const div = $('<div/>', {
			id: plainId([TAB_TYPE.SCENARIOS, 'one', 'back', 'id']),
			class: 'icon-reply'
		});
		div.click(function () {
			clickEventCreator.backToUpperLevel();
		});
		return div;
	}

	return {
		render: render
	}
});

