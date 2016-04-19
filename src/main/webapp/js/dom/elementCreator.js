/**
 * Created on 19.04.16.
 */
define([
	'jquery',
	'../util/templatesUtil'
], function ($, templatesUtil) {

	const TAB_TYPE = templatesUtil.tabTypes();
	const plainId = templatesUtil.composeId;

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
	return {
		treeFormElem: createFormForTree
	}
});
