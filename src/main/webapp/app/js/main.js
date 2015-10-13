require(["./requirejs/conf"], function() {
	require(["handlebars", "text!../templates/navbar.hbs", "text!../templates/conf.hbs",
				"bootstrap", "util/pace/loading", "util/bootstrap/tabs"],
		function(Handlebars, versionTemplate, confTemplate) {

			var context = {
				version: "1.0.0"
			};

			var compiledTemplate = Handlebars.compile(versionTemplate);
			var html = compiledTemplate(context);

			document.body.insertAdjacentHTML("afterbegin", html);

			compiledTemplate = Handlebars.compile(confTemplate);
			html = compiledTemplate(context);

			document.getElementById("configuration").insertAdjacentHTML("beforeend", html);
		}
	);
});