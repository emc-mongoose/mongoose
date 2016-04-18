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
		const TREE_ELEM = templatesUtil.configTreeElements();
		const PATH_DELIMITER = '/';
		const PROPERTY_DELIMITER = '.';
		//
		var currentScenarioJson;
		//
		function setup(configObject, scenariosArray) {
			render(configObject, scenariosArray);
		}

		function render(configObject, scenariosArray) {
			cssUtil.hide(jqId([TAB_TYPE.SCENARIOS, 'one', 'id']));
			hbUtil.compileAndInsertInside(jqId(['main', 'content']), extendedConfTemplate);
			//
			const elemAppender = elementAppenderFactory();
			//
			var rootTreeUlElem = $(jqId([BLOCK.TREE, TAB_TYPE.DEFAULTS]));
			var addressObject = {};
			elemAppender.objectAsTree(configObject, rootTreeUlElem, 'prop', addressObject, PROPERTY_DELIMITER);
			//
			rootTreeUlElem = $(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS]));
			const clickEventCreator = clickEventCreatorFactory();
			elemAppender.arrayAsTree(scenariosArray, rootTreeUlElem, 'dir', PATH_DELIMITER, clickEventCreator.scenarioFile);
		}

		function openedJsonFileProcess(scenarioJson, fullFileName) {
			currentScenarioJson = scenarioJson;
			updateScenarioTree(scenarioJson);
			$(jqId(['config', 'file', 'name', TAB_TYPE.SCENARIOS])).val(fullFileName);
		}

		
		
		const elementCreatorFactory = function () {
			function createFormForTree(addressObj, treeType, delimiter) {
				const form = $('<form/>', {
					id: plainId([treeType, 'main', 'form']),
					class: 'form-horizontal'
				});
				$.each(addressObj, function (key, value) {
					const formGroupDiv = $('<div/>', {
						id: key,
						class: 'form-group'
					});
					const formGroupDivId = jqId([key]);
					const label = $('<label/>', {
						for: key,
						class: 'col-sm-3 control-label '  + plainId(['form', TAB_TYPE.SCENARIOS, 'property']),
						text: key.split(delimiter).slice(-1)
					});
					formGroupDiv.append(label);
					const inputDiv = $('<div/>', {
						class: 'col-sm-9'
					});
					const input = $('<input/>', {
						type: 'text',
						class: 'form-control',
						name: key,
						value: value,
						placeholder: "Enter '" + key + "' property"
					});
					formGroupDiv.append(inputDiv);
					inputDiv.append(input);
					form.append(formGroupDiv);
					formGroupDiv.hide();
				});
				return form;
			}
			function createBackIcon() {
				const div = $('<div/>', {
					id: plainId([TAB_TYPE.SCENARIOS, 'one', 'back', 'id']),
					class: 'icon-reply'
				});
				div.click(function () {
					backClickEvent();
				});
				return div;
			}
			return {
				backIconElem: createBackIcon,
				treeFormElem: createFormForTree
			}
		};
		
		const elementAppenderFactory = function () {
			function fillLeafLi(liElem, aHref, aText, aClickEvent, aClickEventParam) {
				liElem.addClass(TREE_ELEM.LEAF);
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
			function fillNodeLi(liElem, inputId, labelText) {
				liElem.addClass(TREE_ELEM.NODE);
				var input = $('<input/>', {type: 'checkbox', id: inputId});
				var label = $('<label/>', {for: inputId});
				label.text(labelText);
				liElem.append(label, input);
			}
			function itemProcess(item, objCase, notObjCase, rootUlElem) {
				var li = $('<li/>');
				if ((typeof item === 'object') && (item !== null)) {
					objCase(li);
				} else {
					notObjCase(li);
				}
				rootUlElem.append(li);
			}
			// with recursion, pay attention to the internal call if the function signature is being
			// changed
			function addVisualTreeOfObject(object, rootUlElem, nodeIdSuffix,
			                               addressObject, delimiter, elemAddress, aClickEvent) {
				if (!addressObject) {
					addressObject = {};
				}
				if (!elemAddress) {
					elemAddress = '';
				}
				$.each(object, function (key, value) {
					function objCase(li) {
						fillNodeLi(li, plainId([key, nodeIdSuffix, 'id']), key);
						var ul = $('<ul/>');
						li.append(ul);
						const aHrefChunk = key + delimiter;
						addVisualTreeOfObject(value, ul, nodeIdSuffix, addressObject, delimiter,
							elemAddress + aHrefChunk, aClickEvent);
					}

					function notObjCase(li) {
						const addressObjKey = elemAddress + key;
						addressObject[addressObjKey] = value;
						fillLeafLi(li, addressObjKey, key, aClickEvent, addressObjKey);
					}

					itemProcess(value, objCase, notObjCase, rootUlElem);
				})
			}
			// without recursion
			function addVisualTreeOfArray(array, rootUlElem, nodeIdSuffix, delimiter, aClickEvent) {
				$.each(array, function (index, item) {
					function objCase(liOuter) {
						$.each(item, function (nodeName, leavesArr) {
							fillNodeLi(liOuter, plainId([nodeName, nodeIdSuffix, 'id']), nodeName);
							var ul = $('<ul/>');
							$.each(leavesArr, function (index, leafName) {
								var liInner = $('<li/>');
								var aHref = nodeName + delimiter + leafName;
								fillLeafLi(liInner, aHref, leafName, aClickEvent, aHref);
								ul.append(liInner);
							});
							liOuter.append(ul);
						})
					}

					function notObjCase(liOuter) {
						fillLeafLi(liOuter, item, item, aClickEvent, item);
					}

					itemProcess(item, objCase, notObjCase, rootUlElem);
				})
			}
			return {
				arrayAsTree: addVisualTreeOfArray,
				objectAsTree: addVisualTreeOfObject
			}
		};
		
		const elementUpdaterFactory = function () {
			function updateScenarioTree(scenarioJson) {
				const treeUlElem = $(jqId([TAB_TYPE.SCENARIOS, 'one', 'id']));
				treeUlElem.empty();
				treeUlElem.append(createBackIcon());
				cssUtil.addClass(BLOCK.TREE, treeUlElem);
				var addressObject = {};
				addVisualTreeOfObject(scenarioJson, treeUlElem, TREE_ELEM.LEAF, addressObject, PROPERTY_DELIMITER, '', clickScenarioPropertyEvent);
				treeUlElem.show();
				$(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS])).hide();
				$(jqId(['configuration', 'content'])).append(createFormForTree(addressObject, BLOCK.TREE, PROPERTY_DELIMITER));
			}
			return {
				scenarioTree: updateScenarioTree
			}
		};
		
		const clickEventCreatorFactory = function () {
			var prevPropInputId = '';
			function scenarioFileClickEvent(aHref) {
				$.post('/scenario', {path: aHref})
					.done(function (scenarioJson) {
						currentScenarioJson = scenarioJson;
						updateScenarioTree(scenarioJson);
						$(jqId(['config', 'file', 'name', TAB_TYPE.SCENARIOS])).val(aHref);
					})
			}
			function scenarioPropertyClickEvent(aHref) {
				aHref = aHref.replaceAll('\\.', '\\\.');
				const currentPropInputId = jqId([aHref]);
				if (currentPropInputId !== prevPropInputId) {
					cssUtil.hide(prevPropInputId);
					cssUtil.show(currentPropInputId);
					prevPropInputId = currentPropInputId;
				}
			}
			function backClickEvent() {
				cssUtil.show(jqId([BLOCK.TREE, TAB_TYPE.SCENARIOS]));
				cssUtil.hide(jqId([TAB_TYPE.SCENARIOS, 'one', 'id']));
				cssUtil.hide('.' + plainId(['form', TAB_TYPE.SCENARIOS, 'property']));
				$(jqId(['configuration', 'content'])).empty();
				$(jqId(['config', 'file', 'name', TAB_TYPE.SCENARIOS])).val('No scenario chosen');
			}
			return {
				backToUpperLevel: backClickEvent,
				scenarioFile: scenarioFileClickEvent,
				scenarioProperty: scenarioPropertyClickEvent
			}
		};

		return {
			setup: setup,
			openedJsonFileProcess: openedJsonFileProcess
		};
	});
