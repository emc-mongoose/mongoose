define([
	'jquery',
	'./templatesUtil',
	'./cssUtil'
], function ($,
             templatesUtil,
             cssUtil) {

	const TAB_CLASS = templatesUtil.tabClasses();

	function showTabAsActive(tabClassName, tabType) {
		cssUtil.processClassElementsById(tabClassName, tabType,
			function (elemSelector) {
				elemSelector.addClass(TAB_CLASS.ACTIVE);
			},
			function (elemSelector) {
				elemSelector.removeClass(TAB_CLASS.ACTIVE);
			});
	}

	function showTabAsSelected(tabClassName, tabType) {
		cssUtil.processClassElementsById(tabClassName, tabType,
			function (elemSelector) {
				elemSelector.addClass(TAB_CLASS.SELECTED);
			},
			function (elemSelector) {
				elemSelector.removeClass(TAB_CLASS.SELECTED);
			});
	}

	function clickTab(tabClassName, tabType) {
		cssUtil.processClassElementsById(tabClassName, tabType,
			function (elemSelector) {
				elemSelector.trigger('click');
			},
			function (elemSelector) {
			});
	}

	function showActiveTabDependentElements(tabDependentClassName, tabType) {
		cssUtil.processClassElementsById(tabDependentClassName, tabType,
			function (elemSelector) {
				elemSelector.show();
			},
			function (elemSelector) {
				elemSelector.hide();
			});
	}

	function bindTabClickEvent(tabType, tabJqId, makeTabActive) {
		const tabId = tabJqId(tabType);
		$(tabId).click(function () {
			makeTabActive(tabType)
		});
	}

	function bindTabClickEvents(tabObj, tabJqId, makeTabActive) {
		$.each(tabObj, function (key, value) {
			bindTabClickEvent(value, tabJqId, makeTabActive);
		});
	}

	return {
		showTabAsActive: showTabAsActive,
		showTabAsSelected: showTabAsSelected,
		clickTab: clickTab,
		showActiveTabDependentElements: showActiveTabDependentElements,
		bindTabClickEvents: bindTabClickEvents
	}

});