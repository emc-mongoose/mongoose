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
	var hasBeenSetup = false;
	var props;
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
	function setup(properties) {
		props = properties;
		render(); //  render empty fields on HTML page
		renderScenarioModalWindow();
		renderApiModalWindow();
		//
		bindEvents();
		changeLoadHint(SINGLE_SCENARIO);
	}
	//
	function activate() {
		hasBeenSetup = true;
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
	function changeLoadHint(value) {
		var loadHint = $("#scenario-load");
		var scenarioLoad;
		var element;
		if(!hasBeenSetup) {
			value = props.scenario.name;
		}
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
	function bindEvents() {
		//
		var select = $("select");
		select.each(function() {
			var notSelected = $("option:not(:selected)", this);
			notSelected.each(function() {
				var element = $("#" + $(this).val());
				if (element.is("div")) {
					element.hide();
				}
			});
		});
		//
		select.on("change", function() {
			var valueSelected = this.value;
			var notSelected = $("option:not(:selected)", this);
			notSelected.each(function() {
				var element = $("#" + $(this).val());
				if (element.is("div")) {
					element.hide();
				}
			});
			var selectedElement = $("#" + valueSelected);
			if (selectedElement.is("div") && !selectedElement.hasClass("modal")) {
				selectedElement.show();
			}
		});
		//
		$("#base input, #base select").on("change", function() {
			var currElement = $(this);
			//
			var currDataPointer = currElement.attr("data-pointer");
			if(currDataPointer != null && currDataPointer.length > 0) {
				var element = $("#" + currElement.attr("data-pointer").replace(/\./g, "\\.") + " input");
				if(currElement.is("select")) {
					var valueSelected = currElement.children("option").filter(":selected").text().trim();
					$('select[data-pointer="'+currElement.attr("data-pointer")+'"]')
						.val(currElement.val());
					if(element) {
						element.val(valueSelected);
					}
				} else {
					$('input[data-pointer="' + currElement.attr("data-pointer") + '"]')
						.val(currElement.val());
					if(element) {
						element.val(currElement.val());
					}
				}
			}
		});
		//  special event for time field
		$('#duplicate-load\\.limit\\.time input, #duplicate-load\\.limit\\.time select')
			.on("change", function() {
				var strValue = $("#load\\.limit\\.time\\.value").val() +
					$("#load\\.limit\\.time\\.unit").val().charAt(0);
			$("#load\\.limit\\.time").find("input").val(strValue);
		});
		//  aliasing section
		$("#duplicate-data\\.size").on("change", function() {
			//
			var strValue = $("#data\\.size\\.input").val() +
				$("#data\\.size\\.select").val();
			$("#data\\.size\\.min").find("input").val(strValue);
			$("#data\\.size\\.max").find("input").val(strValue);
		});
		//
		$("#duplicate-load\\.connections").on("change", function() {
			var currentValue = this.value;
			var keys2Override = [
				"#duplicate-load\\.type\\.append\\.connections",
				"#duplicate-load\\.type\\.create\\.connections",
				"#duplicate-load\\.type\\.read\\.connections",
				"#duplicate-load\\.type\\.update\\.connections",
				"#duplicate-load\\.type\\.delete\\.connections"
			];
			keys2Override.forEach(function(d) {
				$(d).val(currentValue).change();
			});
			//
			$("#load\\.connections").val(this.value);
		});
	}

	return {
		setup: setup,
		activate: activate
	};

});