define([
	"handlebars",
	"text!../../models/baseConf.json",
	"text!../../../templates/configuration/baseConf.hbs",
	"text!../../../templates/modals/scenarioWindow.hbs",
	"text!../../../templates/modals/apiWindow.hbs"
], function(
	Handlebars,
	baseConfModel,
	baseConfTemplate,
	scenarioWindow,
	apiWindow
) {
	//  variables for modal windows
	var SCENARIO_FIELD_KEY = "duplicate-scenario.name",
		API_FIELD_KEY = "duplicate-api.name";
	//  scenarios
	var SINGLE_SCENARIO = "single",
		CHAIN_SCENARIO = "chain",
		RAMPUP_SCENARIO = "rampup";
	//  modal prefix
	var MODAL_SINGLE_SCENARIO = "modal-single",
		MODAL_CHAIN_SCENARIO = "modal-chain";
	//
	function setup(props) {
		render(); //  render empty fields on HTML page
		renderScenarioModalWindow();
		renderApiModalWindow();
		//
		bindEvents();
	}
	//
	function activate() {
		//  show base configuration fields
		$("#base").show();
		//  hide extended configuration fields
		$(".folders").hide();
		$("#configuration-content").children().hide();
	}
	//
	function render() {
		var compiled = Handlebars.compile(baseConfTemplate);
		var html = compiled(JSON.parse(baseConfModel));
		document.querySelector("#main-content")
			.insertAdjacentHTML("afterbegin", html);
	}
	//
	function renderScenarioModalWindow() {
		var compiled = Handlebars.compile(scenarioWindow);
		var html = compiled();
		document.getElementById(SCENARIO_FIELD_KEY)
			.parentNode.insertAdjacentHTML("afterend", html);
		//
		changeLoadHint(SINGLE_SCENARIO);
		//
		$("#" + MODAL_SINGLE_SCENARIO)
			.find("select").on("change", function() {
				changeLoadHint(SINGLE_SCENARIO);
		});
		$("#" + MODAL_CHAIN_SCENARIO)
			.find("input").on("change", function() {
				changeLoadHint(CHAIN_SCENARIO);
		});
		$("#chain-load").click(function() {
			$("#modal-chain").modal('show').css("z-index", 5000);
		});
		//
		$("#" + SCENARIO_FIELD_KEY.replace(/\./g, "\\."))
			.on("change", function() {
				var valueSelected = this.value;
				document.querySelector("#scenario-button")
					.setAttribute("data-target", "#modal-" + valueSelected);
				changeLoadHint(valueSelected);
		});
	}
	//
	function renderApiModalWindow() {
		var compiled = Handlebars.compile(apiWindow);
		var html = compiled();
		document.getElementById(API_FIELD_KEY)
			.parentNode.insertAdjacentHTML("afterend", html);
		//
		$("#" + API_FIELD_KEY.replace(/\./g, "\\.")).on("change", function() {
			var valueSelected = this.value;
			document.querySelector("#api-button")
				.setAttribute("data-target", "#modal-" + valueSelected);
		});
	}
	//
	function changeLoadHint(value) {
		var loadHint = $("#scenario-load");
		var scenarioLoad;
		var element;
		switch(value) {
			case SINGLE_SCENARIO:
				element = document
					.getElementById("duplicate-scenario.type." + value + ".load");
				scenarioLoad = element.options[element.selectedIndex].text;
				break;
			case CHAIN_SCENARIO:
			case RAMPUP_SCENARIO:
				element = document
					.getElementById("duplicate-scenario.type." + CHAIN_SCENARIO + ".load");
				scenarioLoad = element.value;
				break;
		}
		//
		loadHint.text("Load: [" + scenarioLoad + "]");
	}
	//
	function bindEvents() {
		var select = $("select");
		//
		select.each(function() {
			var notSelected = $("option:not(:selected)", this);
			notSelected.each(function() {
				var current = $("#" + $(this).val());
				if(current.is("div")) {
					current.hide();
				}
			});
			var value = this.value;
			//
			var select = $('select[data-pointer="' + this.id.replace(/\./g, "\\.") + '"]');
			select.val(value);
			select.trigger("change");
			//
			var dataPointer = $(this).attr("data-pointer");
			if(dataPointer) {
				var element = $("#" + dataPointer.replace(/\./g, "\\.")).find("input");
				if(element) {
					element.val(value);
				}
			}
		});
		//
		select.on("change", function() {
			var valueSelected = this.value;
			var notSelected = $("option:not(:selected)", this);
			notSelected.each(function() {
				var current = $("#" + $(this).val());
				if (current.is("div")) {
					current.hide();
				}
			});
			var selectedElement = $("#" + valueSelected);
			if (selectedElement.is("div") && !selectedElement.hasClass("modal")) {
				selectedElement.show();
			} else {
				var value = this.value;
				//
				var select = $('select[data-pointer="' + this.id.replace(/\./g, "\\.") + '"]');
				select.val(value);
				select.trigger("change");
				//
				var dataPointer = $(this).attr("data-pointer");
				if(dataPointer) {
					var element = $("#" + dataPointer.replace(/\./g, "\\.")).find("input");
					if(element) {
						element.val(value);
					}
				}
			}
		});
		//
		var input = $("input");
		//
		input.each(function() {
			var value = this.value;
			//
			var input = $('input[data-pointer="' + this.id.replace(/\./g, "\\.") + '"]');
			input.val(value);
			input.trigger("change");
			//
			var dataPointer = $(this).attr("data-pointer");
			if(dataPointer) {
				var element = $("#" + dataPointer.replace(/\./g, "\\.")).find("input");
				if(element) {
					element.val(value);
				}
			}
		});
		//
		input.on("change", function() {
			var value = this.value;
			//
			var input = $('input[data-pointer="' + this.id.replace(/\./g, "\\.") + '"]');
			input.val(value);
			input.trigger("change");
			//
			var dataPointer = $(this).attr("data-pointer");
			if(dataPointer) {
				var element = $("#" + dataPointer.replace(/\./g, "\\.")).find("input");
				if(element) {
					element.val(value);
				}
			}
		});
	}

	return {
		setup: setup,
		activate: activate
	};

});