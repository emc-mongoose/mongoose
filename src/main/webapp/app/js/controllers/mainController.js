define([
	"handlebars",
	"./configuration/confMenuController",
	"text!../../templates/navbar.hbs",
	"text!../../templates/tabs.hbs"
], function(
	Handlebars,
	confMenuController,
	navbarTemplate,
	tabsTemplate
) {
	//
	function run(props) {
		//  render navbar and tabs before any other interactions
		render(props);
		confMenuController.run(props);
	}
	//
	function render(props) {
		renderNavbar(props.run.version || "unknown");
		renderTabs();
	}
	//
	function renderNavbar(runVersion) {
		var run = {
			version: runVersion
		};
		//
		var compiled = Handlebars.compile(navbarTemplate);
		var navbar = compiled(run);
		document.querySelector("body")
			.insertAdjacentHTML("afterbegin", navbar);
	}
	//
	function renderTabs() {
		var tabs = Handlebars.compile(tabsTemplate);
		document.querySelector("#app")
			.insertAdjacentHTML("afterbegin", tabs());
	}
	//
	return {
		run: run
	};
});