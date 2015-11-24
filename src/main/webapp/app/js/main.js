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
		$.get("/main", function(rtConfig) {
			//  root element ("properties") of mongoose.json configuration file
			var props = rtConfig.properties;
			if(props) {
				mainController.run(props);
			} else {
				alert("Failed to load the configuration");
			}
		});
	});
});