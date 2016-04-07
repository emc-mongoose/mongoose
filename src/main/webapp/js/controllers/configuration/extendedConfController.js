define([
	"handlebars",
	"text!../../../templates/configuration/extendedConf.hbs",
	"../../util/handlebarsShortcuts"
],
function(Handlebars, extendedConfTemplate, HB) {
	//
	var KEY_FIELD_RUN_ID = "run.id",
		KEY_FIELD_LOAD_LIMIT_TIME = "load.limit.time",
		KEY_RUN_MODE = "run.mode";
	//
	function activate() {
		$("#configuration-content").find(".activate").css("display", "block");
	}
	//
	function setup(props) {
		render(props);
	}
	//
	function render(props) {
		HB.compileAndInsert("main-content", 'beforeend', extendedConfTemplate);
		var propsMap = {};
		//  show hidden folders on menu panel
		var ul = $("#folders-defaults");
		addVisualTreeOfObject(ul, props);
		// walkTreeMap(props, ul, propsMap);
		// $("<li>").appendTo($("#run").parent().find("ul").first())
		// 	.addClass("file")
		// 	.append($("<a>", {
		// 		class: "props",
		// 		href: "#" + "run.id",
		// 		text: "id"
		// 	}));
		// //  add empty field which doesn't contain in appConfig
		// propsMap[KEY_FIELD_RUN_ID] = "";
		// buildDivBlocksByPropertyNames(propsMap);
		// bindEvents();
	}
	//
	function onMenuItemClick(element) {
		resetParams();
		if (element.is("a")) {
			var id = element.attr("href").replace(/\./g, "\\.");
			var block = $(id);
			block.addClass("activate");
			block.show();
			block.children().show();
		}
	}
	//
	function resetParams() {
		var content = $("#configuration-content");
		content.children().removeClass("activate");
		content.children().hide();
	}
	//
	function walkTreeMap(map, ul, propsMap, fullKeyName) {
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
				propsMap[currKeyName] = value;
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
				walkTreeMap(value, element, propsMap, currKeyName);
			}
		});
	}
	//
	function addVisualTreeOfObject(rootUlElem, object, compoundName) {
		var aHref;
		if (compoundName) {
			aHref = compoundName;
		} else {
			aHref = '';
		}
		$.each(object, function (key, value) {
			var li = $('<li/>');
			if ((typeof value === 'object') && (value !== null)) {
				li.attr({class: 'dir'});
				const inputId = key + "-prop-id";
				var input = $('<input/>', {type: 'checkbox', id: inputId});
				var label = $('<label/>', {for: inputId});
				label.text(key);
				label.appendTo(li);
				input.appendTo(li);
				var ul = $('<ul/>');
				ul.appendTo(li);
				const delimiter = '.';
				const aHrefChunk = key + delimiter;
				addVisualTreeOfObject(ul, value, aHref + aHrefChunk);
			} else {
				li.attr({class: 'file'});
				var a = $('<a/>', {class: 'props', href: '#' + aHref + key});
				a.text(key);
				a.appendTo(li);
			}
			li.appendTo(rootUlElem);
		})
	}
	//
	function buildDivBlocksByPropertyNames(propsMap) {
		for(var key in propsMap) {
			if(propsMap.hasOwnProperty(key)) {
				if(key === "run.mode")
					continue;
				var keyDiv = $("<div>").addClass("form-group");
				keyDiv.attr("id", key);
				keyDiv.css("display", "none");
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
						value: propsMap[key],
						placeholder: "Enter '" + key + "' property. "
					})));
				keyDiv.appendTo("#configuration-content");
			}
		}
	}
	//
	function bindEvents() {
		$(".folders a, .folders label").click(function(e) {
			e.preventDefault();
			if($(this).is("a")) {
				onMenuItemClick($(this));
			}
		});
		var extended = $("#extended");
		//
		extended.find("input").on("change", function() {
			var parentIdAttr = $(this).parent().parent().attr("id");
			var patternTime = /^([0-9]*)([smhdSMHD]?)$/;
			var numStr = "0", unitStr = "seconds";
			var timeUnitShortCuts = {
				"s" : "seconds",
				"m" : "minutes",
				"h" : "hours",
				"d" : "days"
			};
			if(parentIdAttr == KEY_FIELD_LOAD_LIMIT_TIME) {
				var rawValue = $(this).val();
				if (patternTime.test(rawValue)) {
					var matcher = patternTime.exec(rawValue);
					numStr = matcher[1];
					if (matcher[2] != null && matcher[2].length > 0) {
						unitStr = timeUnitShortCuts[matcher[2].toLowerCase()];
					}
				} else if (rawValue.indexOf('.') > 0) {
					var splitValue = rawValue.split('.');
					numStr = splitValue[0];
					unitStr = splitValue[1];
				}
				// ok, going further
				$("#load\\.limit\\.time\\.value").val(numStr);
				$("#load\\.limit\\.time\\.unit").val(unitStr);
			} else if (parentIdAttr == KEY_RUN_MODE) {
				var runModeElement = $("#run-mode");
				runModeElement.val($(this).val());
				runModeElement.trigger("change");
			} else {
				var input = $('input[data-pointer="' + parentIdAttr + '"]')
					.val($(this).val());
				input.trigger("change");
				var select = $('select[data-pointer="' + parentIdAttr + '"] option:contains(' + $(this)
					.val() + ')')
					.attr('selected', 'selected');
				select.trigger("change");
			}
		});
		//
		$("#run-mode").on("change", function() {
			$("#configuration-content").find("#hidden-run\\.mode").val($(this).val());
		});
		//  activate extended onChange event
		extended.find("input").each(function() {
			$(this).trigger("change");
		});
	}
	//
	return {
		setup: setup,
		activate: activate
	};
});
