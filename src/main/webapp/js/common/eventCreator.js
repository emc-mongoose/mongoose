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

		function propertyClickEvent(aHref) {
			aHref = aHref.replaceAll('\\.', '\\\.');
			const currentPropInputId = jqId([aHref]);
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



	function newClickEventCreator() {
		return clickEventCreatorFactory();
	}

	return {
		newClickEventCreator: newClickEventCreator,
	}
});
