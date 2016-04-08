define([
	"jquery",
	"handlebars",
	"./configuration/confMenuController",
	"./websockets/webSocketController",
	"text!../../templates/navbar.hbs",
	"text!../../templates/run/tab-header.hbs",
	"text!../../templates/run/tab-content.hbs",
	"../util/handlebarsUtil",
	"../util/templatesUtil",
	"../util/cssUtil"
], function ($,
             Handlebars,
             confMenuController,
             webSocketController,
             navbarTemplate,
             tabHeaderTemplate,
             tabContentTemplate,
             hbUtil,
             templatesUtil,
             cssUtil) {

	const TAB_TYPE = templatesUtil.tabTypes();
	const BUTTON_TYPE = templatesUtil.commonButtonTypes();
	const TAB_CLASS = templatesUtil.tabClasses();
	const BLOCK = templatesUtil.blocks();
	
	const jqId = templatesUtil.composeJqId;
	
	function tabJqId(tabType) {
		return jqId('', tabType, 'tab');
	}

	var currentTabType = TAB_TYPE.SCENARIOS;
	//
	function run(configObject, scenariosArray) {
		//  render navbar and tabs before any other interactions
		render(configObject);
		confMenuController.run(configObject, scenariosArray, currentTabType);
		bindTabEvents();
		cssUtil.emptyValue(jqId(BUTTON_TYPE.OPEN_INPUT_TEXT, TAB_TYPE.DEFAULTS));
		cssUtil.disable(jqId(BUTTON_TYPE.OPEN, TAB_TYPE.DEFAULTS));
	}

	function render(configObject) {
		function renderNavbar(runVersion) {
			hbUtil.compileAndInsertInsideBefore('body', navbarTemplate, {version: runVersion});
		}
		renderNavbar(configObject.run.version || 'unknown');
	}

	function bindTabEvents() {
		function makeTabActive(tabType) {
			$.each(TAB_TYPE, function (key, value) {
				const treeId = jqId(BLOCK.TREE, value);
				const buttonsId = jqId(BLOCK.BUTTONS, value);
				if (value === tabType) {
					cssUtil.show(treeId, buttonsId);
					cssUtil.addClass(TAB_CLASS.ACTIVE, tabJqId(tabType))
				} else {
					cssUtil.hide(treeId, buttonsId);
					cssUtil.removeClass(TAB_CLASS.ACTIVE, tabJqId(value));
				}
			});
			currentTabType = tabType;
		}

		function bindTabClickEvent(tabType) {
			const tabId = tabJqId(tabType);
			$(tabId).click(function () {
				makeTabActive(tabType)
			});
		}

		$.each(TAB_TYPE, function (key, value) {
			bindTabClickEvent(value);
		});
		makeTabActive(currentTabType);
	}

	return {
		run: run
	};
});