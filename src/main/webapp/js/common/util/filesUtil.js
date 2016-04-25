/**
 * Created on 25.04.16.
 */
define([
	'jquery',
	'./templatesUtil'
], function ($,
             templatesUtil) {

	const jqId = templatesUtil.composeJqId;

	function changeFileToSaveAs(tabType, content) {
		saveFileAElem = $(jqId(['save', 'file', tabType]));
		if (content !== null) {
			tabType = tabType.slice(0, -1);
			const data = 'text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(content));
			saveFileAElem.attr('href', 'data: ' + data);
			saveFileAElem.attr('download', tabType + '.json')
		} else {
			saveFileAElem.removeAttr('href');
			saveFileAElem.removeAttr('download');

		}
	}

	function objChanger(obj, address, newValue, delimiter) {
		const addressParts = address.split(delimiter).reverse();
		const lastIndex = addressParts.length - 1;
		var tempField = newValue;
		var tempObj = {};
		for (var i = 0; i < lastIndex; i++) {
			tempObj[addressParts[i]] = tempField;
			tempField = tempObj;
		}
		tempObj = {};
		tempObj[addressParts[lastIndex]] = tempField;
		$.extend(true, obj, tempObj);
	}

	return {
		changeFileToSaveAs: changeFileToSaveAs,
		objChanger: objChanger
	}

});