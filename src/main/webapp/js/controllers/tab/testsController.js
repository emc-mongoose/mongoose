/**
 * Created on 18.04.16.
 */
define([
	'jquery',
	'../../common/util/handlebarsUtil',
	'../../common/util/templatesUtil',
	'../../common/util/cssUtil',
	'../../common/util/filesUtil',
	'text!../../../templates/tab/tests/navbar.hbs',
	'text!../../../templates/tab/tests/base.hbs'
], function ($,
             hbUtil,
             templatesUtil,
             cssUtil,
             filesUtil,
             navbarTemplate,
             baseTemplate) {

	const TAB_TYPE = templatesUtil.tabTypes();
	const jqId = templatesUtil.composeJqId;

	function render() {
		const renderer = rendererFactory();
		renderer.navbar();
		renderer.base();
	}

	const rendererFactory = function () {

		const testsBlockElemId = jqId([TAB_TYPE.TESTS, 'block']);

		function renderNavbar() {
			hbUtil.compileAndInsertInsideBefore(testsBlockElemId, navbarTemplate);
		}

		function renderBase() {
			hbUtil.compileAndInsertInside(testsBlockElemId, baseTemplate);
		}

		return {
			navbar: renderNavbar,
			base: renderBase
		}
	};

	function makeTabActive(tabType) {

	}

	return {
		render: render
	}
});
