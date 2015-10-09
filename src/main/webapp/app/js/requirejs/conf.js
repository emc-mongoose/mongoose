/**
 * Created by gusakk on 18.09.15.
 */
requirejs.config({

	"paths": {
		"jquery": "../../app/libs/js/jquery",
		"bootstrap": "../../app/libs/js/bootstrap",
		"handlebars": "../../app/libs/js/handlebars",
		"text": "../../app/libs/js/text"
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