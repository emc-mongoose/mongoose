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
			const addressParts = address.split(delimiter);
			const lastIndex = addressParts.length - 1;
			if (isValueArray(obj, address, delimiter)) {
				newValue = newValue.replaceAll(' ', '').split(',');
			}
			const objTypes = mapObjTypes(obj, address, delimiter);
			if (objTypes.length === 1) {
				obj[address] = newValue;
				return
			}
			var realObj;
			realObj = obj[addressParts[0]];
			for (var i = 1; i < lastIndex; i++) {
				if (objTypes[i]) {
					realObj = realObj[+addressParts[i]];
				} else {
					realObj = realObj[addressParts[i]];
				}
			}
			realObj[addressParts[lastIndex]] = newValue;
		}

		function isValueArray(obj, address, delimiter) {
			const addressParts = address.split(delimiter);
			var value = obj[addressParts[0]];
			for (var i = 1; i < addressParts.length; i++) {
				value = value[addressParts[i]];
			}
			return Array.isArray(value);
		}

		function mapObjTypes(obj, address, delimiter) {
			const addressParts = address.split(delimiter);
			var objTypes = [];
			var value = obj[addressParts[0]];
			if (Array.isArray(value)) {
				objTypes.push(true);
			} else {
				objTypes.push(false);
			}
			for (var i = 1; i < addressParts.length; i++) {
				value = value[addressParts[i]];
				if (Array.isArray(value)) {
					objTypes.push(true);
				} else {
					objTypes.push(false);
				}
			}
			return objTypes.reverse();
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

	}
);