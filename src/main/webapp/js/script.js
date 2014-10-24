$(document).ready(function() {

	var VALUE_RUN_MODE_CLIENT = "VALUE_RUN_MODE_CLIENT";
	var VALUE_RUN_MODE_STANDALONE = "VALUE_RUN_MODE_STANDALONE";

	setStartCookies();
	initComponents();

	$(".add-node").click(function() {
		if ($(".data-node").is(":visible")) {
			$(".data-node").hide();
		} else {
			$(".data-node").show();
		}
	});

	$(".add-driver").click(function() {
		if ($(".driver").is(":visible")) {
			$(".driver").hide();
		} else {
			$(".driver").show();
		}
	});

	$(document).on("click", ".remove", function() {
		$(this).parent().parent().remove();
	});

	$(document).on("click", "#save", function() {
		$(".storages").append(appendBlock("dataNodes", $("#data-node-text").val()));
		$("#data-node-text").val("");
		$(".data-node").hide();
	});

	$(document).on("click", "#save-driver", function() {
		$(".drivers").append(appendBlock("drivers", $("#driver-text").val()));
		$("#driver-text").val("");
		$(".driver").hide();
	});

	$("#distributed").click(function() {
		$(".drivers-block").show();
		$("#runmode").val(VALUE_RUN_MODE_CLIENT);
	});

	$("#standalone").click(function() {
		$(".drivers-block").hide();
		$("#runmode").val(VALUE_RUN_MODE_STANDALONE);
	});


	function setStartCookies() {
		if (!$.cookie("start") && !$.cookie("stop")) {
			$.cookie("start", false);
			$.cookie("stop", true);
		}
	}

	function initComponents() {
		$("#stop").attr("disabled", $.parseJSON($.cookie("stop")));
		$("#start").attr("disabled", $.parseJSON($.cookie("start")));
		$(".data-node").hide();
		$(".driver").hide();
		$(".drivers-block").hide();
	}

	function appendBlock(key, value) {
		html =
			'<div class="input-group">\
				<span class="input-group-addon">\
					<input type="checkbox" name=' + key +' value=' + value + '>\
				</span>\
				<label class="form-control">' +
					value +
				'</label>\
				<span class="input-group-btn">\
					<button type="button" class="btn btn-default remove">Remove</button>\
				</span>\
			</div>';
		return html;
	}

	$(document).on('submit', '#mainForm',  function(e) {
		e.preventDefault();
		$.post("/start", $("#mainForm").serialize(), function(data, status) {
			$.cookie("start", true);
			$.cookie("stop", false);
			$("#start").attr("disabled", $.parseJSON($.cookie("start")));
			$("#stop").attr("disabled", $.parseJSON($.cookie("stop")));
		});
	});

	$("#stop").click(function() {
		$.post("/stop", function(data, status) {
			$.cookie("start", false);
			$.cookie("stop", true);
			$("#start").attr("disabled", $.parseJSON($.cookie("start")));
			$("#stop").attr("disabled", $.parseJSON($.cookie("stop")));
		});
	});
});