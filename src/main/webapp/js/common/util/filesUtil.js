/**
 * Created on 25.04.16.
 */
define([
	'jquery',
	'./templatesUtil'
], function ($,
             templatesUtil) {

	const jqId = templatesUtil.composeJqId;

	function changeFileToSave(tabType, content) {
		saveFileAElem = $(jqId(['save', 'file', tabType]));
		if (content !== null) {
			tabType = tabType.slice(0, -1);
			const data = 'text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(content, null, '\t'));
			saveFileAElem.attr('href', 'data: ' + data);
			saveFileAElem.attr('download', tabType + '.json')
		} else {
			saveFileAElem.removeAttr('href');
			saveFileAElem.removeAttr('download');

		}
	}

	function changeObj(obj, address, newValue, delimiter) {
		const addressParts = address.split(delimiter).reverse();
		const lastIndex = addressParts.length - 1;
		var tempValue = newValue;
		var tempObj;
		for (var i = 0; i < lastIndex; i++) {
			tempObj = {};
			tempObj[addressParts[i]] = tempValue;
			tempValue = tempObj;
		}
		tempObj = {};
		tempObj[addressParts[lastIndex]] = tempValue;
		$.extend(true, obj, tempObj);
	}

	function changeObjAndFile(obj, address, newValue, delimiter, tabType, pElem) {
		changeObj(obj, address, newValue, delimiter);
		changeFileToSave(tabType, obj);
		// pElem.text('MODIFIED');
		pElem.text('');
	}

	// dumb comparison
	function compareObjects(obj1, obj2) {
		return JSON.stringify(obj1) === JSON.stringify(obj2);
	}

	return {
		changeFileToSave: changeFileToSave,
		changeObjAndFile: changeObjAndFile,
		compareObjects: compareObjects
	}

});