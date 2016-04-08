//  Load RequireJS configuration before any other actions
require(['./requirejs/conf'], function() {
	//  App entry point
	require([
		'jquery',
		'./controllers/mainController',
		'./util/templatesUtil',
		'./util/pace/loading',
		'bootstrap',
		'./util/bootstrap/tabs'
	], function($, mainController, templateConstants) {
		
		function extractAppConfig(fullAppJson) {
			return fullAppJson['appConfig']['config'];
		}
		
		function extractScenariosDirContents(fullAppJson) {
			return fullAppJson[templateConstants.tabTypes().SCENARIOS];
		}
		
		//  get all properties from runTimeConfig
		$.get("/main", function(fullAppJson) {
			//  root element ("config") of defaults.json configuration file
			var configObject = extractAppConfig(fullAppJson);
			var scenariosArray = extractScenariosDirContents(fullAppJson);
			if(configObject && scenariosArray) {
				mainController.run(configObject, scenariosArray);
			} else {
				alert('Failed to load the configuration');
			}
		});
	});
});
