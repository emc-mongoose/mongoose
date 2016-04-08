define([
		"handlebars",
		"text!../../../templates/configuration/extendedConf.hbs",
		"../../util/handlebarsUtil",
		"../../util/templatesUtil",
		"../../util/cssUtil"
	],
	function (Handlebars, extendedConfTemplate, hbUtil, templatesUtil, cssUtil) {
		//
		const TAB_TYPE = templatesUtil.tabTypes();
		const BLOCK = templatesUtil.blocks();
		const DELIMITER = '.';
		//
		var currentScenarioJson;
		//
		function setup(configObject, scenariosArray) {
			render(configObject, scenariosArray);
		}

		function getScenarioFromServer(aHref) {
			$.post('/scenario', {path: aHref})
				.done(function (scenarioJson) {
					currentScenarioJson = scenarioJson;
				})
		}

		function updateScenarioTree() {
			var ul = $('#' + templatesUtil.getConstElemId('folders', TAB_TYPE.SCENARIOS));
		}

		//
		function render(configObject, scenariosArray) {
			hbUtil.compileAndInsertInside('#main-content', extendedConfTemplate);
			var ulId = templatesUtil.composeJqId('', BLOCK.TREE, TAB_TYPE.DEFAULTS);
			var ul = $(ulId);
			var addressObject = {};
			addVisualTreeOfObject(configObject, ul, addressObject);
			ulId = templatesUtil.composeJqId('', BLOCK.TREE, TAB_TYPE.SCENARIOS);
			ul = $(ulId);
			addVisualTreeOfArray(scenariosArray, ul, getScenarioFromServer);
		}
		
		function fillFileLi(liElem, aHref, aText, aClickEvent, aClickEventParam) {
			liElem.attr({class: 'file'});
			var a = $('<a/>',
				{
					class: 'props',
					href: '#' + aHref
				});
			a.text(aText);
			if (aClickEvent) {
				a.click(function () {
					aClickEvent(aClickEventParam);
				});
			}
			liElem.append(a);
		}

		//
		function fillDirLi(liElem, inputId, labelText) {
			liElem.attr({class: 'dir'});
			var input = $('<input/>', {type: 'checkbox', id: inputId});
			var label = $('<label/>', {for: inputId});
			label.text(labelText);
			liElem.append(label, input);
		}

		//
		function itemProcess(item, objCase, notObjCase, rootUlElem) {
			var li = $('<li/>');
			if ((typeof item === 'object') && (item !== null)) {
				objCase(li);
			} else {
				notObjCase(li);
			}
			rootUlElem.append(li);
		}

		//
		function addVisualTreeOfObject(object, rootUlElem, addressObject, elemAddress) {
			if (!addressObject) {
				addressObject = {};
			}
			if (!elemAddress) {
				elemAddress = '';
			}
			$.each(object, function (key, value) {
				function objCase(li) {
					fillDirLi(li, key + "-prop-id", key);
					var ul = $('<ul/>');
					li.append(ul);
					const aHrefChunk = key + DELIMITER;
					addVisualTreeOfObject(value, ul, addressObject, elemAddress + aHrefChunk);
				}

				function notObjCase(li) {
					const addressObjKey = elemAddress + key;
					addressObject[addressObjKey] = value;
					fillFileLi(li, addressObjKey, key);
				}

				itemProcess(value, objCase, notObjCase, rootUlElem);
			})
		}

		function addVisualTreeOfArray(array, rootUlElem, aClickEvent) {
			$.each(array, function (index, item) {
				function objCase(liOuter) {
					$.each(item, function (dirName, filesArr) {
						fillDirLi(liOuter, dirName + "-dir-id", dirName);
						var ul = $('<ul/>');
						$.each(filesArr, function (index, fileName) {
							var liInner = $('<li/>');
							var aHref = dirName + DELIMITER + fileName;
							fillFileLi(liInner, aHref, fileName, aClickEvent, aHref);
							ul.append(liInner);
						});
						liOuter.append(ul);
					})
				}

				function notObjCase(liOuter) {
					fillFileLi(liOuter, item, item, aClickEvent, item);
				}

				itemProcess(item, objCase, notObjCase, rootUlElem);
			})
		}
		//
		return {
			setup: setup,
		};
	});
