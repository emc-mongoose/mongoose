/**
 * Created by gusakk on 18.09.15.
 */
requirejs.config({

	"paths": {
		"jquery": "../../libs/js/jquery",
		"bootstrap": "../../libs/js/bootstrap",
		"handlebars": "../../libs/js/handlebars",
		"text": "../../libs/js/text",
		"pace": "../../libs/js/pace",
		"d3js": "../../libs/js/d3js"
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