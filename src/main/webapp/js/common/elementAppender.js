/**
 * Created on 19.04.16.
 */
define([
	'jquery',
	'./util/templatesUtil',
	'./eventCreator',
	'./util/filesUtil'
], function ($,
             templatesUtil,
             eventCreator,
             filesUtil) {

	const TREE_ELEM = templatesUtil.configTreeElements();
	const plainId = templatesUtil.composeId;
	const jqId = templatesUtil.composeJqId;
	const enterKeyCode = 13;

	function fillLeafLi(liElem, aName, aText, aClickEvent) {
		liElem.addClass(TREE_ELEM.LEAF);
		var a = $('<a/>',
			{
				class: 'props',
				path: aName
			});
		a.text(aText);
		if (aClickEvent) {
			a.click(function () {
				aClickEvent(aName);
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

	// function itemProcess(item, objCase, arrCase, notObjCase, rootUlElem) {
	// 	var li = $('<li/>');
	// 	if ((typeof item === 'object') && (item !== null)) {
	// 		if (Array.isArray(item)) {
	// 			arrCase(li);
	// 		} else {
	// 			objCase(li);
	// 		}
	// 	} else {
	// 		notObjCase(li);
	// 	}
	// 	rootUlElem.append(li);
	// }

	const ITEM_TYPE = {
		PLAIN: 'plain',
		OBJECT: 'obj',
		ARRAY: 'arr',
		PLAIN_ARRAY: 'plainArr',
		MIXED_ARRAY: 'mixedArr'
	};

	function isObject(item) {
		return (typeof item === 'object') && (item !== null);
	}

	function isPlain(item) {
		return !isObject(item);
	}

	function isArray(item) {
		return Array.isArray(item);
	}

	function isArrayPlain(arr) {
		for (var i = 0; i < arr.length; i++) {
			if (isObject(arr[i])) {
				return false;
			}
		}
		return true;
	}

	function itemType(item) {
		if (isObject(item)) {
			if (isArray(item)) {
				if (isArrayPlain(item)) {
					return ITEM_TYPE.PLAIN_ARRAY;
				} else {
					return ITEM_TYPE.MIXED_ARRAY;
				}
			} else {
				return ITEM_TYPE.OBJECT;
			}
		} else {
			return ITEM_TYPE.PLAIN;
		}
	}


	function singleItemProcess(item, objCase, plainCase) {
		if (isObject(item)) {
			objCase(item);
		} else {
			plainCase(item);
		}
	}

	function itemProcess(item, objCase, plainCase) {
		if (isArray(item)) {
			item.forEach(function (singleItem) {
				singleItemProcess(singleItem, objCase, plainCase);
			});
		} else {
			singleItemProcess(item, objCase, plainCase);
		}
	}


	function addTreeOfItem(item, rootElem, delimiter, aClickEvent, arrAsNode) {

		function plainPairAsNode(itemElem, nodeName, leafItem) {
			fillNodeLi(itemElem, plainId([nodeName, 'id']), nodeName);
			var leavesElem = $('<ul/>');
			itemElem.append(leavesElem);
			if (isArray(leafItem)) {
				leafItem.forEach(function (leafName) {
					addTreeOfItem(leafName, leavesElem, delimiter, aClickEvent, arrAsNode);
				});
			} else {
				addTreeOfItem(leafItem, leavesElem, delimiter, aClickEvent, arrAsNode);
			}
		}

		function plainPairAsLeaf(itemElem, leafName, fieldValue) {
			fillLeafLi(itemElem, plainId([leafName, 'id']), leafName, aClickEvent);
		}

		function objectPair(itemElem, nodeName, leafItem) {
			fillNodeLi(itemElem, plainId([nodeName, 'id']), nodeName);
			var leavesElem = $('<ul/>');
			itemElem.append(leavesElem);
			addTreeOfItem(leafItem, leavesElem, delimiter, aClickEvent, arrAsNode);
		}

		function objCase(item) {
			$.each(item, function (key, value) {
				const newItemElem = $('<li/>');
				rootElem.append(newItemElem);
				const valueType = itemType(value);
				switch (valueType) {
					case ITEM_TYPE.OBJECT:
						objectPair(newItemElem, key, value);
						break;
					case ITEM_TYPE.PLAIN:
					case ITEM_TYPE.PLAIN_ARRAY:
						if (arrAsNode) {
							plainPairAsNode(newItemElem, key, value);
						} else {
							plainPairAsLeaf(newItemElem, key, value);
						}
						break;
					case ITEM_TYPE.MIXED_ARRAY:
						break;
				}
			})
		}

		function plainCase(item) {
			const newItemElem = $('<li/>');
			rootElem.append(newItemElem);
			fillLeafLi(newItemElem, item, item, aClickEvent);
		}

		itemProcess(item, objCase, plainCase);
	}

	// with recursion, pay attention to the internal call if the function signature is being
	// changed
	// function addVisualTreeOfObject(object, rootUlElem, nodeIdSuffix,
	//                                addressObject, delimiter, elemAddress, aClickEvent, arrayValuesHandling) {
	// 	if (!addressObject) {
	// 		addressObject = {};
	// 	}
	// 	if (!elemAddress) {
	// 		elemAddress = '';
	// 	}
	// 	$.each(object, function (key, item) {
	//
	// 		function objCase(li) {
	// 			fillNodeLi(li, plainId([key, nodeIdSuffix, 'id']), key);
	// 			var ul = $('<ul/>');
	// 			li.append(ul);
	// 			const aNameChunk = key + delimiter;
	// 			addVisualTreeOfObject(item, ul, nodeIdSuffix, addressObject, delimiter,
	// 				elemAddress + aNameChunk, aClickEvent);
	// 		}
	//
	// 		function notObjCase(li) {
	// 			if (Array.isArray(object)) {
	// 				if (arrayValuesHandling) {
	//
	// 				} else {
	//
	// 				}
	// 			} else {
	// 				const addressObjKey = elemAddress + key;
	// 				addressObject[addressObjKey] = item;
	// 				fillLeafLi(li, addressObjKey, key, aClickEvent);
	// 			}
	// 		}
	//
	// 		itemProcess(item, objCase, notObjCase, rootUlElem);
	// 	})
	// }
	//
	// // without recursion
	// function addVisualTreeOfArray(array, rootUlElem, nodeIdSuffix, delimiter, aClickEvent) {
	// 	$.each(array, function (index, item) {
	// 		function objCase(liOuter) {
	// 			liOuter.remove();
	// 			addVisualTreeOfObject(item, rootUlElem, nodeIdSuffix, null, delimiter, null, aClickEvent, false);
	// 			// $.each(item, function (nodeName, leavesArr) {
	// 			// 	fillNodeLi(liOuter, plainId([nodeName, nodeIdSuffix, 'id']), nodeName);
	// 			// 	var ul = $('<ul/>');
	// 			// 	$.each(leavesArr, function (index, leafName) {
	// 			// 		var liInner = $('<li/>');
	// 			// 		var aName = nodeName + delimiter + leafName;
	// 			// 		fillLeafLi(liInner, aName, leafName, aClickEvent, aName);
	// 			// 		ul.append(liInner);
	// 			// 	});
	// 			// 	liOuter.append(ul);
	// 			// })
	// 		}
	//
	// 		function notObjCase(liOuter) {
	// 			fillLeafLi(liOuter, item, item, aClickEvent);
	// 		}
	//
	// 		itemProcess(item, objCase, notObjCase, rootUlElem);
	// 	})
	// }

	function addFormForTree(addressObj, rootFormElem, delimiter, objectToChangeWithForm, tabType, jsonViewElem) {
		const enterWarning = 'Press enter to commit a change';
		$.each(addressObj, function (key, value) {
			const formGroupDiv = $('<div/>', {
				id: key,
				class: 'form-group'
			});
			const p = $('<p/>', {
				class: 'enter-warning'
			});
			p.append(enterWarning);
			formGroupDiv.append(p);
			const label = $('<label/>', {
				for: key,
				class: 'col-sm-3 control-label',
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
			input.change(function () {
				filesUtil.changeObjAndFile(objectToChangeWithForm, key, input.val(), delimiter, tabType, p);
				if (jsonViewElem) {
					jsonViewElem.text(JSON.stringify(objectToChangeWithForm, null, '\t'));
				}
			});
			input.keydown(function (event) {
				switch (event.keyCode) {
					case enterKeyCode:
						input.trigger('change');
						return false;
				}
			});
			formGroupDiv.append(inputDiv);
			inputDiv.append(input);
			rootFormElem.append(formGroupDiv);
			formGroupDiv.hide();
		});
	}

	return {
		// arrayAsTree: addVisualTreeOfArray,
		// objectAsTree: addVisualTreeOfObject,
		// formForTree: addFormForTree,
		treeOfItem: addTreeOfItem
	}
});