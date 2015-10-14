require(["./requirejs/conf"], function() {
	require(["handlebars", "jquery", "text!../templates/navbar.hbs", "text!../templates/conf.hbs",
				"text!./configuration/base.json",
				"bootstrap", "util/pace/loading", "util/bootstrap/tabs"],
		function(Handlebars, $, versionTemplate, confTemplate, config) {

			var context = {
				version: "1.0.0"
			};

			var compiledTemplate = Handlebars.compile(versionTemplate);
			var html = compiledTemplate(context);

			document.body.insertAdjacentHTML("afterbegin", html);

			compiledTemplate = Handlebars.compile(confTemplate);

			config = JSON.parse(config);

			html = compiledTemplate(config);

			document.getElementById("configuration").insertAdjacentHTML("beforeend", html);

			document.querySelector("#file-checkbox").addEventListener("change", function() {
				var file = document.querySelector("#config-file");
				if (this.checked) {
					file.style.display = "block";
				} else {
					file.style.display = "none";
				}
			});

			$.get("/main", function(json) {
				traverse(json);
			});

			//  fill the configuration w/ default values
			function traverse(jsonObject, fieldPrefix) {
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
									fillComplexElement(domElement, jsonObject[key]);
								}
								domElement.value = jsonObject[key];
							}
						} else {
							traverse(jsonObject[key], propertyName);
						}
					}
				}
			}

			function fillComplexElement(domElement, value) {
				var regExpPattern = /^([0-9]*)([a-zA-Z]+)$/;
				if (regExpPattern.test(value)) {
					var matcher = regExpPattern.exec(value);
					domElement.querySelector("input").value = matcher[1];
					var selectableElement = domElement.querySelector("select");
					var options = selectableElement.options;
					for (var option in options) {
						if (options.hasOwnProperty(option)) {
							option = options[option];
							var searchPattern = new RegExp('^' + matcher[2]);
							if (searchPattern.test(option.innerText)) {
								selectableElement.value = option.innerText;
								domElement.querySelector('input[type="hidden"]').value
									= matcher[1] + matcher[2];
							}
						}
					}
				}
			}
		}
	);
});