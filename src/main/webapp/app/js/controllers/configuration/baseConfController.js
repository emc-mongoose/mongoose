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
	//
	function start(props) {
		//
		$("#extended").remove();
		var folders = $(".folders");
		folders.children().remove();
		folders.hide();
		//  render empty fields on HTML page
		render();

		addModalWindows();
		addApiModalWindow();

		//  fill these fields w/ values from trConfig
		traverseJsonTree(props);

		bindEventsOnAliasingProps();

		var select = $("select");

		select.each(function() {
			var notSelected = $("option:not(:selected)", this);
			notSelected.each(function() {
				var current = $("#" + $(this).val());
				if (current.is("div")) {
					current.hide();
				}
			});
			var duplicate = document.getElementById("duplicate-" + this.id);
			if (duplicate) {
				duplicate.value = this.value;
				return;
			}
			var dataPointer = $(this).attr("data-pointer");
			if (dataPointer) {
				document.getElementById(dataPointer).value = this.value;
			}
		});

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
				var duplicate = document.getElementById("duplicate-" + this.id);
				if (duplicate) {
					duplicate.value = this.value;
					return;
				}
				var dataPointer = $(this).attr("data-pointer");
				if (dataPointer) {
					document.getElementById(dataPointer).value = this.value;
				}
			}
		});

		var input = $("input");

		input.each(function() {
			var duplicate = document.getElementById("duplicate-" + this.id);
			if (duplicate) {
				duplicate.value = this.value;
				return;
			}
			var dataPointer = $(this).attr("data-pointer");
			if (dataPointer) {
				document.getElementById(dataPointer).value = this.value;
			}
		});

		input.on("change", function() {
			var duplicate = document.getElementById("duplicate-" + this.id);
			if (duplicate) {
				duplicate.value = this.value;
				return;
			}
			var dataPointer = $(this).attr("data-pointer");
			if (dataPointer) {
				document.getElementById(dataPointer).value = this.value;
			}
		});
	}

	function render() {
		var compiled = Handlebars.compile(baseConfTemplate);
		var html = compiled(JSON.parse(baseConfModel));

		//  show base configuration fields
		document.querySelector("#main-content")
			.insertAdjacentHTML("afterbegin", html);
	}

	function traverseJsonTree(jsonObject, fieldPrefix) {
		for (var key in jsonObject) {
			if (jsonObject.hasOwnProperty(key)) {
				var propertyName = "";
				if (fieldPrefix != null) {
					if (fieldPrefix.length > 0) {
						propertyName = fieldPrefix + "." + key;
					} else {
						propertyName = key;
					}
				}

				if (!(typeof jsonObject[key] === "object")) {
					var domElement = document.getElementById(propertyName);
					if (domElement) {
						if (domElement.classList.contains("complex")) {
							fillComplexConfigField(domElement, jsonObject[key]);
						}
						fillConfigField(domElement, jsonObject[key]);
					}
				} else {
					traverseJsonTree(jsonObject[key], propertyName);
				}
			}
		}
	}

	function fillConfigField(domeElement, value) {
		domeElement.value = value;
		$(domeElement).trigger("change");
	}

	function fillComplexConfigField(domElement, value) {
		var regExpPattern = /^([0-9]*)([a-zA-Z]+)$/;
		if (regExpPattern.test(value)) {
			var matcher = regExpPattern.exec(value);
			var inputField = domElement.querySelector("input");
			inputField.value = matcher[1];
			$(inputField).trigger("change");
			//
			var selectableElement = domElement.querySelector("select");
			var options = selectableElement.options;
			for (var option in options) {
				if (options.hasOwnProperty(option)) {
					option = options[option];
					var searchPattern = new RegExp('^' + matcher[2]);
					if (searchPattern.test(option.innerText)) {
						selectableElement.value = option.innerText;
						$(selectableElement).trigger("change");
						domElement.querySelector('input[type="hidden"]').value
								= matcher[1] + matcher[2];
					}
				}
			}
			//
			inputField.addEventListener("change", function () {
				var parentDiv = findAncestor(this, "complex");
				var selectableElement = parentDiv.querySelector("select");
				parentDiv.querySelector('input[type="hidden"]').value =
						this.value + selectableElement.options[selectableElement.selectedIndex].value;
			});
			//
			selectableElement.addEventListener("change", function () {
				var parentDiv = findAncestor(this, "complex");
				var inputElement = parentDiv.querySelector("input");
				parentDiv.querySelector('input[type="hidden"]').value =
						inputElement.value + this.options[this.selectedIndex].value;
			});
		}
	}

	function findAncestor(el, cls) {
		while ((el = el.parentElement) && !el.classList.contains(cls));
		return el;
	}

	function bindEventsOnAliasingProps() {

		var dataSize = {
			id: "data.size",
			aliasFor: [
				"data.size.min",
				"data.size.max"
			]
		};

		$("#base").find("form")
				.append("<input type='hidden' " +
				"name='" + dataSize.aliasFor[0] +
				"' id='" + dataSize.aliasFor[0] + "'/>");

		$("#base").find("form")
				.append("<input type='hidden' " +
				"name='" + dataSize.aliasFor[1] +
				"' id='" + dataSize.aliasFor[1] + "'/>");

		var loadConnections = {
			id: "load.connections",
			aliasFor: [
				"load.type.create.connections",
				"load.type.read.connections",
				"load.type.update.connections",
				"load.type.delete.connections",
				"load.type.append.connections"
			]
		};
		//
		fillComplexConfigField(
			document.getElementById(dataSize.id), document.getElementById(dataSize.aliasFor[0]).value
		);
		//
		fillConfigField(
			document.getElementById(loadConnections.id),
			document.getElementById(loadConnections.aliasFor[0]).value
		);
		//
		var parentDiv = document.getElementById(dataSize.id);
		//
		parentDiv.querySelector("input").addEventListener("change", function() {
			var parentDiv = findAncestor(this, "complex");
			var selectableElement = parentDiv.querySelector("select");
			parentDiv.querySelector('input[type="hidden"]').value =
				this.value + selectableElement.options[selectableElement.selectedIndex].value;
		});
		//
		parentDiv.querySelector("select").addEventListener("change", function() {
			var parentDiv = findAncestor(this, "complex");
			var inputElement = parentDiv.querySelector("input");
			//
			var value = inputElement.value + this.options[this.selectedIndex].value;
			//
			parentDiv.querySelector('input[type="hidden"]').value =
				value;
			for (var element in dataSize.aliasFor) {
				if (dataSize.aliasFor.hasOwnProperty(element)) {
					var current = document.getElementById(dataSize.aliasFor[element]);
					/*if (!current) {

					}*/
					document.getElementById(
						dataSize.aliasFor[element]
					).value = value;
				}
			}
		});
		//


		document.getElementById(loadConnections.id).addEventListener("change", function() {
			var value = this.value;
			for (var element in loadConnections.aliasFor) {
				if (loadConnections.aliasFor.hasOwnProperty(element)) {
					document.getElementById(
						loadConnections.aliasFor[element]
					).value = value;
				}
			}
		})
	}

	function addModalWindows() {
		var compiled = Handlebars.compile(scenarioWindow);
		var html = compiled();
		document.getElementById("scenario.name")
			.parentNode.insertAdjacentHTML("afterend", html);
		changeLoadHint("single");
		//
		$("#single").find("select").on("change", function() {
			changeLoadHint("single");
		});
		//
		$("#chain").find("input").on("change", function() {
			changeLoadHint("chain");
		});
		//
		document.getElementById("scenario.name").addEventListener("change", function() {
			var valueSelected = this.value;
			document.querySelector("#scenario-button")
				.setAttribute("data-target", "#" + valueSelected);
			changeLoadHint(valueSelected);
		});


		$("#chain-load").click(function() {
			$("#chain").modal('show').css("z-index", 5000);
		});

	}

	function addApiModalWindow() {
		var compiled = Handlebars.compile(apiWindow);
		var html = compiled();
		document.getElementById("api.name")
			.parentNode.insertAdjacentHTML("afterend", html);

		//
		document.getElementById("api.name").addEventListener("change", function() {
			var valueSelected = this.value;
			document.querySelector("#api-button")
				.setAttribute("data-target", "#" + valueSelected);
		});
	}

	function changeLoadHint(value) {
		var loadHint = $("#scenario-load");
		//
		var scenarioTypeSingleLoad = document.getElementById("scenario.type.single.load").value;
		var scenarioTypeChainLoad = document.getElementById("scenario.type.chain.load").value;
		switch (value) {
			case "single":
				loadHint.text("Load: [" + scenarioTypeSingleLoad + "]");
				break;
			case "chain":
				loadHint.text("Load: [" + scenarioTypeChainLoad + "]");
				break;
			case "rampup":
				loadHint.text("Load: [" + scenarioTypeChainLoad + "]");
				break;
		}
	}

	return {
		fillConfigField: fillConfigField,
		fillComplexConfigField: fillComplexConfigField,
		start: start
	};

});