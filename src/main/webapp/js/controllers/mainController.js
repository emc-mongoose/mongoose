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
	function run(config, scenariosArray) {
		//  render navbar and tabs before any other interactions
		render(config);
		confMenuController.run(config, currentTabType, runIdArray, scenariosArray);
		bindTabEvents();
	}

	function render(config) {
		function renderNavbar(runVersion) {
			const navbarHtml = HB.compile(navbarTemplate, { version: runVersion });
			document.querySelector("body").insertAdjacentHTML('afterbegin', navbarHtml);
			$('#' + tabId(currentTabType)).addClass(TAB_CLASS.ACTIVE);
			bindTabEvents();
		}
		renderNavbar(config.run.version || "unknown");
	}

	function bindTabEvents() {
		function showSuitableButtons(tabType) {
			if (currentTabType != tabType) {
				$('#buttons-' + currentTabType).hide();
				$('#buttons-' + tabType).show();
			}
		}
		function makeTabActive(tabType) {
			if (currentTabType != tabType) {
				$.each(TAB_TYPE, function (key, value) {
					$('#' + tabId(value)).removeClass(TAB_CLASS.ACTIVE);
				});
				$('#' + tabId(tabType)).addClass(TAB_CLASS.ACTIVE);
				showSuitableButtons(tabType);
				currentTabType = tabType;
			}
			if (tabType == TAB_TYPE.SCENARIOS) {
				$("#start").show();
			} else {
				$("#start").hide();
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
		$("#config-file-name-defaults").val("");
	}
	
	return {
		run: run
	};
});