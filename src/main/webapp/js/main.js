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
		
		function getAppConfig(fullAppJson) {
			return fullAppJson['appConfig']['config'];
		}
		
		function getScenariosDirContents(fullAppJson) {
			return fullAppJson["scenarios"];
		}
		
		//  get all properties from runTimeConfig
		$.get("/main", function(fullAppJson) {
			//  root element ("config") of defaults.json configuration file
			var config = getAppConfig(fullAppJson);
			var scenariosArray = getScenariosDirContents(fullAppJson);
			if(config && scenariosArray) {
				mainController.run(config, scenariosArray);
			} else {
				alert("Failed to load the configuration");
			}
		});
	});
});
