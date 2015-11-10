define([
	"handlebars",
	"./configuration/confMenuController",
	"text!../../templates/navbar.hbs",
	"text!../../templates/tabs.hbs"
], function(
	Handlebars, confMenuController, navbarTemplate, tabsTemplate
) {

	function start(rtConfig) {

		var run = {
			version: rtConfig.run.version || "unknown"
		};

		//  render navbar and tabs before any other actions
		render(run);

		confMenuController.start(rtConfig);

	}

	function render(content) {

		var compiled = Handlebars.compile(navbarTemplate);
		var navbar = compiled(content);

		var tabs = Handlebars.compile(tabsTemplate);

		//  Navbar
		document.querySelector("body")
			.insertAdjacentHTML("afterbegin", navbar);
		//  Tabs
		document.querySelector("#app")
			.insertAdjacentHTML("afterbegin", tabs());
		
	}

	return {
		start: start
	};
});