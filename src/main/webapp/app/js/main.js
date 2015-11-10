// Load RequireJS configuration before any other actions
require(["./requirejs/conf"], function() {
	//  App entry point
	require([
		"jquery",
		"./controllers/defaultController",
		"./util/pace/loading",
		"bootstrap",
		"./util/bootstrap/tabs"
	], function($, defaultController) {
		$.get("/main", function(rtConfig) {
			if (rtConfig) {
				defaultController.start(rtConfig.properties);
			}
		});
	});
});