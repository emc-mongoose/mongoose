/**
 * Created on 20.04.16.
 */
define([
	'jquery',
	'../util/templatesUtil',
	'../util/cssUtil'
], function ($,
             templatesUtil,
             cssUtil) {

	const jqId = templatesUtil.composeJqId;

	const clickEventCreatorFactory = function () {

		var prevPropInputId = '';

		function propertyClickEvent(aName) {
			aName = aName.replaceAll('\\.', '\\\.');
			const currentPropInputId = jqId([aName]);
			if (currentPropInputId !== prevPropInputId) {
				cssUtil.hide(prevPropInputId);
				cssUtil.show(currentPropInputId);
				prevPropInputId = currentPropInputId;
			} else {
				cssUtil.show(currentPropInputId);
			}
		}

		return {
			propertyClickEvent: propertyClickEvent
		}

	};

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

	function newClickEventCreator() {
		return clickEventCreatorFactory();
	}

	return {
		newClickEventCreator: newClickEventCreator,
		changeFileToSaveAs: changeFileToSaveAs
	}
});
