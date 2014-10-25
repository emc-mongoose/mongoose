$(document).ready(function() {

	var VALUE_RUN_MODE_CLIENT = "VALUE_RUN_MODE_CLIENT";
	var VALUE_RUN_MODE_STANDALONE = "VALUE_RUN_MODE_STANDALONE";

	setStartCookies();
	initComponents();
	initWebSocket();

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

	function initWebSocket() {
		var server = {
			connect: function() {
				var location = document.location.toString().replace('http://', 'ws://') + "logs";
				this._ws = new WebSocket(location);
				this._ws.onopen = this._onopen;
				this._ws.onmessage = this._onmessage;
				this._ws.onclose = this._onclose;
			},
			_onopen : function() {
				server._send('websockets!');
			},
			_send : function(message) {
				if (this._ws) {
					this._ws.send(message);
				}
			},
			send : function(text) {
				if (text != null && text.length > 0)
					server._send(text);
			},
			_onmessage : function(m) {
				var json = JSON.parse(m.data);
				switch (json.marker.name) {
					case "msg":
						alert("A");
				}
				//$("#messages-csv").append("<p>" + json.marker.name + "</p>");

			},
			_onclose : function(m) {
				this._ws = null;
			}
		};
		server.connect();
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