define([
	"jquery",
	"handlebars",
	"./configuration/confMenuController",
	"./websockets/webSocketController",
	"text!../../templates/navbar.hbs",
	"text!../../templates/run/tab-header.hbs",
	"text!../../templates/run/tab-content.hbs",
	"../util/handlebarsShortcuts",
	"../util/templateConstants"
], function ($,
             Handlebars,
             confMenuController,
             webSocketController,
             navbarTemplate,
             tabHeaderTemplate,
             tabContentTemplate,
             HB,
             TEMPLATE) {
	
	const TAB_TYPE = TEMPLATE.tabTypes();

	function tabId(tabType) {
		return tabType + '-tab';
	}

	const TAB_CLASS = {
		ACTIVE: 'active'
	};

	var currentTabType = TAB_TYPE.SCENARIOS;
	var runIdArray = [];
	//
	function run(configObject, scenariosArray) {
		//  render navbar and tabs before any other interactions
		render(configObject);
		confMenuController.run(configObject, scenariosArray, currentTabType, runIdArray);
		bindTabEvents();
		$("#config-file-name-" + TAB_TYPE.DEFAULTS).val(""); // at the request of a customer
	}

	function render(config) {
		function renderNavbar(runVersion) {
			const navbarHtml = HB.compile(navbarTemplate, {version: runVersion});
			document.querySelector("body").insertAdjacentHTML('afterbegin', navbarHtml);
			$('#' + tabId(currentTabType)).addClass(TAB_CLASS.ACTIVE);
			bindTabEvents();
		}

		renderNavbar(config.run.version || "unknown");
	}

	function bindTabEvents() {
		
		var tabTypeElemId = TEMPLATE.getConstElemId;

		function showElemById(id) {
			$("#" + id).show();
		}

		function hideElemById(id) {
			$("#" + id).hide();
		}

		function makeTabActive(tabType) {
			$.each(TAB_TYPE, function (key, value) {
				const foldersId = TEMPLATE.getConstElemId('folders', value);
				const buttonsId = TEMPLATE.getConstElemId('buttons', value);
				if (value === tabType) {
					showElemById(foldersId);
					showElemById(buttonsId);
					$('#' + tabId(tabType)).addClass(TAB_CLASS.ACTIVE);
				} else {
					hideElemById(foldersId);
					hideElemById(buttonsId);
					$('#' + tabId(value)).removeClass(TAB_CLASS.ACTIVE);
				}
			});
			currentTabType = tabType;
		}

		function bindTabClickEvent(tabType) {
			$('#' + tabId(tabType)).click(function () {
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