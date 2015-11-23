// Load RequireJS configuration before any other actions
require(["./requirejs/conf"], function() {
	//  App entry point
	require([
		"jquery",
		"./controllers/mainController",
		"./util/pace/loading",
		"bootstrap",
		"./util/bootstrap/tabs"
	], function($, mainController) {
		$.get("/main", function(rtConfig) {
			var props = rtConfig.properties; //  root element of mongoose.json file ("properties")
			if(props) {
				mainController.start(props);
			}
		});
	});
});