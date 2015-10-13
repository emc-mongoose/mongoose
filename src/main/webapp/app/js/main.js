require(["./requirejs/conf"], function() {
	require(["handlebars", "text!../templates/navbar.hbs", "text!../templates/conf.hbs",
				"text!./configuration/base.json",
				"bootstrap", "util/pace/loading", "util/bootstrap/tabs"],
		function(Handlebars, versionTemplate, confTemplate, config) {

			var context = {
				version: "1.0.0"
			};

			var compiledTemplate = Handlebars.compile(versionTemplate);
			var html = compiledTemplate(context);

			document.body.insertAdjacentHTML("afterbegin", html);

			compiledTemplate = Handlebars.compile(confTemplate);
			html = compiledTemplate(JSON.parse(config));

			document.getElementById("configuration").insertAdjacentHTML("beforeend", html);

			document.querySelector("#file-checkbox").addEventListener("change", function() {
				var file = document.querySelector("#config-file");
				if (this.checked) {
					file.style.display = "block";
				} else {
					file.style.display = "none";
				}
			});
		}
	);
});