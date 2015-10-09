require(["./requirejs/conf"], function() {
	require(["handlebars", "bootstrap", "text!../templates/body.hbs"],
		function(Handlebars, bootstrap, template) {
			var context = {
				title: "My New Post",
				body: "This is my first post!"
			};

			var result = Handlebars.compile(template);
			var html = result(context);

			document.getElementById("app").insertAdjacentHTML("beforeend", html);
		}
	);
});