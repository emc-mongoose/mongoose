define([
	"jquery",
	"handlebars",
	"text!../../../templates/configuration/confMenu.hbs",
	"text!../../../templates/configuration/commonButtons.hbs",
	"./extendedConfController",
	"../run/runController",
	"../../common/util/handlebarsUtil",
	"../../common/util/templatesUtil",
	"../../common/util/cssUtil"
], function(
	$,
	Handlebars,
	confMenuTemplate,
	commonButtonsTemplate,
    extendedConfController,
    runController,
	hbUtil,
    templatesUtil,
    cssUtil
) {
	const jqId = templatesUtil.composeJqId;
	
	const TAB_TYPE = templatesUtil.tabTypes();
	const BUTTON_TYPE = templatesUtil.commonButtonTypes();
	const BLOCK = templatesUtil.blocks();
	// object to array
	const CONFIG_TABS = $.map(TAB_TYPE, function (value) { return value; }).slice(0, 2);
	const BUTTONS = $.map(BUTTON_TYPE, function (value) { return value; });
	
	function run(configObject, scenariosArray, currentTabType) {
		render(currentTabType);
		extendedConfController.setup(configObject, scenariosArray);
	}
	//
	function render(currentTabType) {
		function renderConfMenu() {
			hbUtil.compileAndInsertAfter('header', confMenuTemplate, { 'tab-types': CONFIG_TABS });
		}
		function renderCommonButtons() {
			$.each(CONFIG_TABS, function (index, value) {
				hbUtil.compileAndInsertInsideBefore('#config', commonButtonsTemplate,
					{ 'buttons': BUTTONS, 'tab-type': value });
			});
		}
		function bindOpenButtonEvent() {
			
			function buttonJqId(buttonType, tabName) {
				return jqId([buttonType, tabName]);
			}
			
			function passClick(tabName) {
				const openInputFileId = buttonJqId(BUTTON_TYPE.OPEN_INPUT_FILE, tabName);
				$(buttonJqId(BUTTON_TYPE.OPEN, tabName)).click(function () {
					$(openInputFileId).trigger('click')
				});
				$(openInputFileId).change(openFileEvent);
			}
			
			function fillTheField(tabName) {
				const openInputFileId = buttonJqId(BUTTON_TYPE.OPEN_INPUT_FILE, tabName);
				const openInputTextId = buttonJqId(BUTTON_TYPE.OPEN_INPUT_TEXT, tabName);
				const openFileName = $(openInputFileId).val();
				if (openFileName) {
					$(openInputTextId).val(openFileName)
				} else {
					$(openInputTextId).val('No ' + tabName.slice(0, -1) + ' chosen')
				}
			}
			$.each(CONFIG_TABS, function (index, value) {
				passClick(value);
				fillTheField(value);
			});
			
		}
		function hideExtraButtons(currentTabType) {
			$.each(CONFIG_TABS, function (index, value) {
				if (value != currentTabType) {
					const buttonBlockId = jqId([BLOCK.BUTTONS, value]);
					cssUtil.hide(buttonBlockId);
				}
			})
		}
		
		var fullFileName = '';
		
		const reader = new FileReader();
		reader.onload = function (data) {
			content = data.target.result;
			const json = JSON.parse(content);
			extendedConfController.openedJsonFileProcess(json, fullFileName);
		};
		reader.onerror = function (data) {
			console.error("File couldn't be read. Code" + data.target.error.code);
		};
		
		function openFileEvent(data) {
			const files = data.target.files; // FileList object
			const file = files[0];
			fullFileName = file.name;
			reader.readAsText(file);
		}
		
		renderConfMenu();
		renderCommonButtons();
		bindOpenButtonEvent();
		hideExtraButtons(currentTabType)
	}
	
	return {
		run: run
	};
});
