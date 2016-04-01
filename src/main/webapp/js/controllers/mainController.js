define([
	"jquery",
	"handlebars",
	"./configuration/confMenuController",
	"./websockets/webSocketController",
	"text!../../templates/navbar.hbs",
	"text!../../templates/run/tab-header.hbs",
	"text!../../templates/run/tab-content.hbs"
], function(
	$,
	Handlebars,
	confMenuController,
	webSocketController,
	navbarTemplate,
	tabHeaderTemplate,
	tabContentTemplate
) {
	const TAB_TYPE = {
		SCENARIOS: 'scenarios',
		DEFAULTS: 'defaults',
		TESTS: 'tests',
		OTHER: 'other'
	};

	function tabId(tabType) {
		return tabType + '-tab';
	}

	const TAB_CLASS = {
		ACTIVE: 'active'
	};

	var currentTabType;
	var runIdArray = [];
	//
	function run(config) {
		//  render navbar and tabs before any other interactions
		render(config);
		bindTabEvents();
		currentTabType = TAB_TYPE.SCENARIOS;
		confMenuController.run(config, currentTabType, runIdArray);
	}

	function render(config) {
		renderNavbar(config.run.version || "unknown");
	}

	function renderNavbar(runVersion) {
		var run = {
			version: runVersion
		};
		//
		var compiled = Handlebars.compile(navbarTemplate);
		var navbar = compiled(run);
		document.querySelector("body").insertAdjacentHTML('afterbegin', navbar);
		$("#scenarios-tab").addClass('active');
	}

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
	
	return {
		run: run
	};
});