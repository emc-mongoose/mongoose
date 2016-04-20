/**
 * Created on 19.04.16.
 */
define([
	'jquery',
	'../util/templatesUtil'
], function ($, templatesUtil) {

	const TREE_ELEM = templatesUtil.configTreeElements();
	const plainId = templatesUtil.composeId;

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
	
	function addFormForTree(addressObj, rootFormElem, delimiter) {
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
			rootFormElem.append(formGroupDiv);
			formGroupDiv.hide();
		});
	}

	return {
		arrayAsTree: addVisualTreeOfArray,
		objectAsTree: addVisualTreeOfObject
	}
});