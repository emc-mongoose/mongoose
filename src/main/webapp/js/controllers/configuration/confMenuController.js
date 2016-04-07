define([
	"jquery",
	"handlebars",
	"text!../../../templates/configuration/confMenu.hbs",
	"text!../../../templates/configuration/commonButtons.hbs",
	"./extendedConfController",
	"../run/runController",
	"../../util/handlebarsShortcuts",
	"../../util/templateConstants"
], function(
	$,
	Handlebars,
	confMenuTemplate,
	commonButtonsTemplate,
    extendedConfController,
    runController,
	HB,
    TEMPLATE
) {
	//
	function run(configObject, scenariosArray, currentTabType, tabRunIdArray) {
		//  default run.mode ("webui") from appConfig should be overridden here
		var run = {
			mode: "standalone" // possible: ["standalone", "client", "server", "cinderella"]
		};
		//  render configuration menu panel
		render(currentTabType, scenariosArray);
		extendedConfController.setup(configObject, scenariosArray);
		//  some settings for configuration menu
		bindMenuEvents(configObject, tabRunIdArray);
	}
	//
	function render(currentTabType, scenariosArray) {
		const CONFIG_TABS = TEMPLATE.configTabs();
		const BUTTONS = TEMPLATE.commonButtons();
		function renderConfMenu(configTabs, scenariosArray) {
			HB.compileAndInsert('header', 'afterend', confMenuTemplate, { 'tab-types': configTabs, 'scenarios': scenariosArray });
		}
		function renderCommonButtons(configTabs) {
			const htmlButtonSet1 = HB.compile(commonButtonsTemplate, { 'buttons': BUTTONS, 'tab-type': configTabs[0]});
			const htmlButtonSet2 = HB.compile(commonButtonsTemplate, { 'buttons': BUTTONS, 'tab-type': configTabs[1]});
			HB.insert('config', 'afterbegin', htmlButtonSet2);
			HB.insert('config', 'afterbegin', htmlButtonSet1);
		}
		function bindOpenButtonEvent(configTabs) {
			const BUTTON_TYPE = TEMPLATE.commonButtonTypes();
			function passClick(tabName) {
				$('#' + BUTTON_TYPE.OPEN + '-' + tabName).click(function () {
					$('#' + BUTTON_TYPE.OPEN_INPUT_FILE + '-' + tabName).trigger('click')
				})
			}
			var fillTheField = function (tabName) {
				const openInputFileId = '#' + BUTTON_TYPE.OPEN_INPUT_FILE + '-' + tabName;
				const openInputTextId = '#' + BUTTON_TYPE.OPEN_INPUT_TEXT + '-' + tabName;
				const openFileName = $(openInputFileId).val();
				if (openFileName) {
					$(openInputTextId).val(openFileName)
				} else {
					$(openInputTextId).val('No ' + tabName.slice(0, -1) + ' chosen')
				}
			};
			$.each(configTabs, function (index, value) {
				passClick(value);
				fillTheField(value);
			})
		}
		function hideExtraButtons(currentTabType, configTabs) {
			$.each(configTabs, function (index, value) {
				if (value != currentTabType) {
					$('#buttons-' + value).hide();
				}
			})
		}
		renderConfMenu(CONFIG_TABS, scenariosArray);
		renderCommonButtons(CONFIG_TABS);
		bindOpenButtonEvent(CONFIG_TABS);
		hideExtraButtons(currentTabType, CONFIG_TABS)
	}
	

	//
	function bindMenuEvents(props, runIdArray) {
		//  config mode change
		var configModeSelect = $("#config-type");
		configModeSelect.on("change", function() {
			extendedConfController.activate();
		});
		//  activate
		configModeSelect.trigger("change");

		////////////////////////////////////////////////////////////////////////////////////////////

		//  run mode change (show base conf fields only for selected run mode)
		var runModeSelect = $("#run-mode");
		runModeSelect.on("change", function() {
			var valueSelected = this.options[this.selectedIndex].value;
			var notSelected = $("option:not(:selected)", this);
			notSelected.each(function() {
				$("." + this.value).hide();
			});
			$("." + valueSelected).show();
		});
		//  activate
		runModeSelect.trigger("change");

		////////////////////////////////////////////////////////////////////////////////////////////

		var fields = $("#main-form").serialize();

		//  update config file (save button click)
		var updateConfig = $("#save-config");
		updateConfig.click(function() {
			$.post("/save", fields, function() {
				alert("Config was successfully saved");
			});
		});

		//  save configuration in separate file
		var saveInFile = $("#save-file");
		saveInFile.click(function(e) {
			e.preventDefault();
			$.get("/save", fields, function() {
				window.location.href = "/save/config.json";
			});
		});

		////////////////////////////////////////////////////////////////////////////////////////////

		//  read cfg from file
		var fileInput = $("#config-file");
		fileInput.change(function() {
			var input = $(this).get(0);
			loadPropertiesFromFile(props, input.files[0]);
		});
	}

	function loadPropertiesFromFile(props, fileName) {
		var reader = new FileReader();
		reader.onload = function() {
			var result = reader.result;
			var lines = result.split("\n");
			var key, value;
			for(var i = 0;i < lines.length; i++) {
				var splitLine = lines[i].split(" = ");
				//  key and value
				if(splitLine.length == 2) {
					key = splitLine[0];
					value = splitLine[1];
					index(props, key, value);
				} else {
					return;
				}
			}
		};
		reader.readAsText(fileName);
	}

	function index(obj,is, value) {
		try {
			if(typeof is == 'string')
				return index(obj, is.split('.'), value);
			else if(is.length == 1 && value !== undefined)
				return obj[is[0]] = value;
			else if(is.length == 0)
				return obj;
			else
				return index(obj[is[0]], is.slice(1), value);
		} catch(e) {
			//  do nothing
		}
	}

	return {
		run: run
	};
});
