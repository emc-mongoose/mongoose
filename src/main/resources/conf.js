/**
 * Created by gusakk on 18.09.15.
 */
requirejs.config({
	"paths": {
		"bootstrap": ["/webjars/bootstrap/3.3.2-1/js/bootstrap","js/bootstrap"],
		"d3js": ["/webjars/d3js/3.5.3/d3","d3"],
		"validate-js": ["/webjars/validate.js/0.8.0/validate"],
		"jquery": ["/webjars/jquery/2.1.4/jquery","jquery"]
	},
	"shim": {
		"bootstrap":["jquery"],
		"d3js":{"exports":"d3"},
		"jquery":{"exports":"$"}
	},
	"packages": []
});