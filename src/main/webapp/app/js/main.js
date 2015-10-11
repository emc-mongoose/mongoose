require(["./requirejs/conf"], function() {
	require(["handlebars", "text!../templates/navbar.hbs", "bootstrap", "util/pace/loading"],
		function(Handlebars, versionTemplate) {
			var context = {
				version: "1.0.0"
			};

			var compiledTemplate = Handlebars.compile(versionTemplate);
			var html = compiledTemplate(context);

			document.body.insertAdjacentHTML("afterbegin", html);
		}
	);
});