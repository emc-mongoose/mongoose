define([
	"jquery",
	"handlebars",
	"text!../../../templates/configuration/confMenu.hbs",
	"./baseConfController",
	"./extendedConfController",
	"../run/runController"
], function(
	$,
	Handlebars,
	confMenuTemplate,
    baseConfController,
    extendedConfController,
    runController
) {
	//
	function run(props) {
		//  default run.mode ("webui") from rtConfig should be overridden here
		var run = {
			mode: "standalone" // possible: ["standalone", "client", "server", "cinderella"]
		};
		//  render configuration menu panel
		render();
		baseConfController.setup(props);
		extendedConfController.setup(props);
		//  some settings for configuration menu
		setDefaultRunMode(run.mode);
		bindMenuEvents(props);
	}
	//
	function render() {
		var compiled = Handlebars.compile(confMenuTemplate);
		document.querySelector("#configuration")
			.insertAdjacentHTML("afterbegin", compiled());
	}
	//
	function setDefaultRunMode(runMode) {
		var select = document.querySelector("#run-mode");
		var options = select.options;
		for(var option in options) {
			if(options.hasOwnProperty(option)) {
				option = options[option];
				if(option.value == runMode) {
					select.value = option.value;
				}
			}
		}
	}
	//
	function bindMenuEvents(props) {
		//
		$("#start").on("click", function(e) {
			e.preventDefault();
			runController.start();
		});
		//  config mode change
		var configTypes = {
			"base": baseConfController,
			"extended": extendedConfController
		};
		var configModeSelect = $("#config-type");
		configModeSelect.on("change", function() {
			var activeOptionValue = this.options[this.selectedIndex].value;
			//  activate baseConfController or extendedConfController
			configTypes[activeOptionValue].activate();
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

		//  show input field for reading cfg from file
		var fileCheckbox = $("#file-checkbox");
		fileCheckbox.on("change", function() {
			var file = document.querySelector("#config-file");
			if(this.checked) {
				file.style.display = "block";
			} else {
				file.style.display = "none";
			}
		});

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
				window.location.href = "/save/config.txt";
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