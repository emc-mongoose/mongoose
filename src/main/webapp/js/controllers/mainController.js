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
], function(
	$,
	Handlebars,
	confMenuController,
	webSocketController,
	navbarTemplate,
	tabHeaderTemplate,
	tabContentTemplate,
	HB,
    TEMPLATE
) {
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
	function run(config) {
		//  render navbar and tabs before any other interactions
		render(config);
		confMenuController.run(config, runIdArray);
	}

	function render(config) {
		function bindTabEvents() {
			function makeTabActive(tabType) {
				if (currentTabType != tabType) {
					$.each(TAB_TYPE, function (key, value) {
						$('#' + tabId(value)).removeClass(TAB_CLASS.ACTIVE);
					});
					$('#' + tabId(tabType)).addClass(TAB_CLASS.ACTIVE);
					currentTabType = tabType;
				}
			}
			function bindTabClickEvent(tabType) {
				$('#' + tabId(tabType)).click(function() {
					makeTabActive(tabType)
				});
			}
			$.each(TAB_TYPE, function(key, value) {
				bindTabClickEvent(value);
			});
		}
		function renderNavbar(runVersion) {
			const navbarHtml = HB.compile(navbarTemplate, { version: runVersion });
			document.querySelector("body").insertAdjacentHTML('afterbegin', navbarHtml);
			$('#' + tabId(currentTabType)).addClass(TAB_CLASS.ACTIVE);
			bindTabEvents();
		}
		renderNavbar(config.run.version || "unknown");
	}
	
	return {
		run: run
	};
});