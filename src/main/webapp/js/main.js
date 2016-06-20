//  Load RequireJS configuration before any other actions
require(['./requirejs/conf'], function() {
	//  App entry point
	require([
		'jquery',
		'./controllers/mainController',
		'./common/util/templatesUtil',
		'./util/pace/loading',
		'bootstrap',
		'./util/bootstrap/tabs'
	], function($, mainController, templateConstants) {

		String.prototype.replaceAll = function(search, replacement) {
			var target = this;
			return target.replace(new RegExp(search, 'g'), replacement);
		};

		const dataExtractorFactory = function (fullAppJson) {
			function extractAppConfig() {
				return fullAppJson['appConfig']['config'];
			}

			function extractScenariosDirContents() {
				return fullAppJson[templateConstants.tabTypes().SCENARIOS];
			}
			
			return {
				appConfig: extractAppConfig,
				scenarioDirContents: extractScenariosDirContents
			}
		};
		
		function sortObjByKeys(obj) {
			const newObj = {};
			const keys = Object.keys(obj);
			keys.sort();
			keys.forEach(function (key) {
				newObj[key] = obj[key];
			});
			return newObj;
		}
		
		//  get all properties from runTimeConfig
		$.get("/main", function(fullAppJson) {
			//  root element ("config") of defaults.json configuration file
			const dataExtractor = dataExtractorFactory(fullAppJson);
			const configObject = dataExtractor.appConfig();
			const scenariosArray = dataExtractor.scenarioDirContents();
			if(configObject && scenariosArray) {
				mainController.render(scenariosArray, configObject);
			} else {
				alert('Failed to load the configuration');
			}
		});
	});
});
