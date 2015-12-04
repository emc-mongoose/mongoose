/**
 * Created by gusakk on 18.09.15.
 */
requirejs.config({

	"paths": {
		"bootstrap": ["/webjars/bootstrap/3.3.2-1/js/bootstrap", "js/bootstrap"],
		"d3js": ["/webjars/d3js/3.5.3/d3", "d3"],
		"validate-js": ["/webjars/validate.js/0.8.0/validate"],
		"jquery": ["/webjars/jquery/2.1.4/jquery", "jquery"],
		"handlebars": ["/webjars/handlebars/4.0.2/handlebars", "handlebars"],
		"pace": ["/webjars/pace/1.0.2/pace", "pace"],
		"text": ["/webjars/requirejs-text/2.0.14-1/text", "text"]
	},

	"shim": {
		"bootstrap": {
			"deps": ["jquery"],
			"exports": "bootstrap"
		},
		"handlebars": {
			"exports": "Handlebars"
		}
	}
});