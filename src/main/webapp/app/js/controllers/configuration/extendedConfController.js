define(["handlebars"], function(Handlebars) {
	//
	function start(props) {
		render(props);
	}
	//
	function render(props) {
		var shortPropsMap = {};
		var ul = $(".folders");
		ul.show();
		walkTreeMap(props, ul, shortPropsMap);
		$("<li>").appendTo($("#run").parent().find("ul").first())
			.addClass("file")
			.append($("<a>", {
				class: "props",
				href: "#" + "run.id",
				text: "id"
			}));
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
	return {
		start: start
	};
});