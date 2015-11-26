define([
	"handlebars",
	"text!../../../templates/configuration/extendedConf.hbs"
],
function(Handlebars, extendedConf) {
	//
	function activate() {
		$("#base").hide();
		$(".folders").show();
		$("#configuration-content").find(".activate").css("display", "block");
	}
	//
	function setup(props) {
		//render(props);
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
		shortPropsMap["run.id"] = "";
		buildDivBlocksByPropertyNames(shortPropsMap);
		bindEvents();
		//
		$("#run-mode").trigger("change");
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
		//
		var extended = $("#extended");
		//
		extended.find("input").on("change", function() {
			var value = this.value;
			var parentIdAttr = $(this).parent().parent().attr("id");
			//  find duplicated elements
			var div = $("#duplicate-" + parentIdAttr.replace(/\./g, "\\."));
			if(div.is("div")) {
				var regExpPattern = /^([0-9]*)([a-zA-Z]+)$/;
				if(regExpPattern.test(value)) {
					var matcher = regExpPattern.exec(value);
					var inputField = div.find("input");
					inputField.val(matcher[1]);
					inputField.trigger("change");
					//
					var selectableElement = div.find("select").get(0);
					var options = selectableElement.options;
					for(var option in options) {
						if(options.hasOwnProperty(option)) {
							option = options[option];
							var searchPattern = new RegExp('^' + matcher[2]);
							if(searchPattern.test(option.innerText)) {
								selectableElement.value = option.innerText;
								$(selectableElement).trigger("change");
							}
						}
					}
				}
			}
			//
			var input = $('input[data-pointer="' + parentIdAttr + '"]');
			input.val(value);
			input.trigger("change");
			//
			var select = $('select[data-pointer="' + parentIdAttr + '"]');
			select.val(value);
			try {
				if (select.find('option[value=modal-"' + value + '"]').length > 0) {
					select.val("modal-" + value);
				}
			} catch(err) {

			}
			select.trigger("change");
			//
		});
		//
		//
		extended.find("input").each(function() {
			$(this).trigger("change");
		});
		//
		$("#run-mode").on("change", function() {
			$("#configuration-content").find("#hidden-run\\.mode").val($(this).val());
		});
	}
	//
	return {
		setup: setup,
		activate: activate
	};
});