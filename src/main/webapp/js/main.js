//  Load RequireJS configuration before any other actions
require(["./requirejs/conf"], function() {
	//  App entry point
	require([
		"jquery",
		"./controllers/mainController",
		"./util/pace/loading",
		"bootstrap",
		"./util/bootstrap/tabs"
	], function($, mainController) {
		//  get all properties from runTimeConfig
		$.get("/main", function(appConfig) {
			//  root element ("config") of defaults.json configuration file
			var config = appConfig.config;
			if(config) {
				mainController.run(config);
			} else {
				alert("Failed to load the configuration");
			}
		});
	});
});
