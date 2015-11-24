define([
	"handlebars",
	"text!../../../templates/configuration/extendedConf.hbs"
],
function(Handlebars, extendedConf) {
	//
	function start(props) {
		//
		$("#base").remove();
		//
		render(props);
	}
	//
	function render(props) {
		var compiled = Handlebars.compile(extendedConf);
		var html = compiled();
		document.querySelector("#main-content")
			.insertAdjacentHTML("beforeend", html);
		//
		var shortPropsMap = {};
		var ul = $(".folders");
		ul.show();
		//
		walkTreeMap(props, ul, shortPropsMap);
		$("<li>").appendTo($("#run").parent().find("ul").first())
			.addClass("file")
			.append($("<a>", {
				class: "props",
				href: "#" + "run.id",
				text: "id"
			}));
		//
		buildDivBlocksByPropertyNames(shortPropsMap);
		bindEvents();
	}
	//
	function bindEvents() {
		$(".folders a, .folders label").click(function(e) {
			if ($(this).is("a")) {
				e.preventDefault();
			}
			//
			onMenuItemClick($(this));
		});
	}
	//
	function onMenuItemClick(element) {
		resetParams();
		element.css("color", "#CC0033");
		if (element.is("a")) {
			var id = element.attr("href").replace(/\./g, "\\.");
			var block = $(id);
			block.show();
			block.children().show();
		}
	}
	//
	function resetParams() {
		$("a, label").css("color", "");
		$("#configuration-content").children().hide();
	}
	//
	function walkTreeMap(map, ul, shortsPropsMap, fullKeyName) {
		$.each(map, function(key, value) {
			var element;
			var currKeyName = "";
			//
			if(!fullKeyName) {
				currKeyName = key;
			} else {
				currKeyName = fullKeyName + "." + key;
			}
			//
			if (!(value instanceof Object)) {
				if(currKeyName === "run.mode")
					return;
				$("<li>").appendTo(ul)
					.addClass("file")
					.append($("<a>", {
						class: "props",
						href: "#" + currKeyName,
						text: key
					}));
				shortsPropsMap[currKeyName] = value;
			} else {
				element = $("<ul>").appendTo(
					$("<li>").prependTo(ul)
						.append($("<label>", {
							for: key,
							text: key
						}))
						.append($("<input>", {
							type: "checkbox",
							id: key
						}))
					);
				walkTreeMap(value, element, shortsPropsMap, currKeyName);
			}
		});
	}
	//
	function buildDivBlocksByPropertyNames(shortPropsMap) {
		for(var key in shortPropsMap) {
			if(shortPropsMap.hasOwnProperty(key)) {
				if(key === "run.mode")
					continue;
				var keyDiv = $("<div>").addClass("form-group");
				keyDiv.attr("id", key);
				keyDiv.css("display", "none");
				var placeHolder = "";
				if (key === "data.src.fpath") {
					placeHolder = "Format: log/<run.mode>/<run.id>/<filename>";
				}
				keyDiv.append($("<label>", {
					for: key,
					class: "col-sm-3 control-label",
					text: key.split(".").pop()
				}))
					.append($("<div>", {
						class: "col-sm-9"
					}).append($("<input>", {
						type: "text",
						class: "form-control",
						name: key,
						value: shortPropsMap[key],
						placeholder: "Enter '" + key + "' property. " + placeHolder
					})));
				keyDiv.appendTo("#configuration-content");
			}
		}
	}
	//
	return {
		start: start
	};
});