define(["pace", "jquery"], function(pace, $) {
	pace.on("done", function() {
		$(".cover").fadeOut(500);
	});

	pace.start({
		document: false
	});
});