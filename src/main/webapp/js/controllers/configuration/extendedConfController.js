define([
		"handlebars",
		"text!../../../templates/configuration/extendedConf.hbs",
		"../../util/handlebarsUtil",
		"../../util/templatesUtil",
		"../../util/cssUtil"
	],
	function (Handlebars, extendedConfTemplate, hbUtil, templatesUtil, cssUtil) {

		const plainId = templatesUtil.composeId;
		const jqId = templatesUtil.composeJqId;
		
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
					updateScenarioTree(scenarioJson);
				})
		}

		function backClickEvent() {
			cssUtil.show(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS]));
			cssUtil.hide(jqId([TAB_TYPE.SCENARIOS, 'one', 'id']));
		}

		function updateScenarioTree(scenarioJson) {
			const ul = $(jqId([TAB_TYPE.SCENARIOS, 'one', 'id']));
			ul.empty();
			const div = $('<div/>');
			// todo check
			cssUtil.addIdByElem(plainId([TAB_TYPE.SCENARIOS, 'one', 'back', 'id']), div);
			cssUtil.addClass('icon-reply', div);
			ul.append(div);
			$(jqId([TAB_TYPE.SCENARIOS, 'one', 'back', 'id'])).click(function () {
				backClickEvent();
			});
			cssUtil.addClass(BLOCK.TREE, ul);
			var addressObject = {};
			addVisualTreeOfObject(scenarioJson, ul, '-file-id', addressObject);
			cssUtil.show(jqId([TAB_TYPE.SCENARIOS, 'one', 'id']));
			cssUtil.hide(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS]));
			const form = createFormForTree(addressObject);
			$(jqId(['configuration', 'content'])).append(form);
		}

		//
		function render(configObject, scenariosArray) {
			cssUtil.hide(jqId([TAB_TYPE.SCENARIOS, 'one', 'id']));
			hbUtil.compileAndInsertInside('#main-content', extendedConfTemplate);
			var ul = $(jqId([BLOCK.TREE, TAB_TYPE.DEFAULTS]));
			var addressObject = {};
			addVisualTreeOfObject(configObject, ul, '-prop-id', addressObject);
			ul = $(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS]));
			addVisualTreeOfArray(scenariosArray, ul, '-dir-id', getScenarioFromServer);
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
		function addVisualTreeOfObject(object, rootUlElem, nodeIdSuffix, addressObject, elemAddress) {
			if (!addressObject) {
				addressObject = {};
			}
			if (!elemAddress) {
				elemAddress = '';
			}
			$.each(object, function (key, value) {
				function objCase(li) {
					fillDirLi(li, key + nodeIdSuffix, key);
					var ul = $('<ul/>');
					li.append(ul);
					const aHrefChunk = key + DELIMITER;
					addVisualTreeOfObject(value, ul, nodeIdSuffix, addressObject, elemAddress + aHrefChunk);
				}

				function notObjCase(li) {
					const addressObjKey = elemAddress + key;
					addressObject[addressObjKey] = value;
					fillFileLi(li, addressObjKey, key);
				}

				itemProcess(value, objCase, notObjCase, rootUlElem);
			})
		}

		function addVisualTreeOfArray(array, rootUlElem, nodeIdSuffix, aClickEvent) {
			$.each(array, function (index, item) {
				function objCase(liOuter) {
					$.each(item, function (dirName, filesArr) {
						fillDirLi(liOuter, dirName + nodeIdSuffix, dirName);
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

		function createFormForTree(addressObj) {
			const form = $('<form/>');
			const formPlainId = plainId([BLOCK.TREE, 'main', 'form']);
			cssUtil.addIdByElem(formPlainId, form);
			cssUtil.addClass('form-horizontal');
			$.each(addressObj, function (key, value) {
				const formGroupDiv = $('<div/>');
				const formGroupDivId = jqId([key]);
				cssUtil.addIdByElem(key, formGroupDiv);
				cssUtil.addClass('form-group', formGroupDivId);
				const label = $('<label/>');
				label.attr('for', key);
				label.addClass('col-sm-3');
				label.addClass('control-label');
				label.text(key.split('.').slice(-1));
				formGroupDiv.append(label);
				const inputDiv = $('<div/>');
				inputDiv.addClass('col-sm-9');
				const input = $('<input/>');
				input.attr('type', 'text');
				input.addClass('form-control');
				input.attr('name', key);
				input.val(value);
				input.attr('placeholder', "Enter '" + key + "' property");
				formGroupDiv.append(inputDiv);
				inputDiv.append(input);
				form.append(formGroupDiv);
			});
			return form;
		}

		return {
			setup: setup
		};
	});
