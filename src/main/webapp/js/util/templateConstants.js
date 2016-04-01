/**
 * Created on 01.04.16.
 */
define(function() {

    const TAB_TYPE = {
        SCENARIOS: 'scenarios',
        DEFAULTS: 'defaults',
        TESTS: 'tests',
        OTHER: 'other'
    };

    const COMMON_BUTTON_TYPE = {
        OPEN: 'open',
        OPEN_INPUT_TEXT: 'config-file-name',
        OPEN_INPUT_FILE: 'config-file',
        SAVE: 'save-config',
        SAVE_AS: 'save-file'
    };

    const CONFIG_TABS = [TAB_TYPE.SCENARIOS, TAB_TYPE.DEFAULTS];
    const COMMON_BUTTONS = [
        COMMON_BUTTON_TYPE.OPEN, COMMON_BUTTON_TYPE.OPEN_INPUT_TEXT,
        COMMON_BUTTON_TYPE.OPEN_INPUT_FILE, COMMON_BUTTON_TYPE.SAVE,
        COMMON_BUTTON_TYPE.SAVE_AS];

    function tabTypes() {
        return TAB_TYPE;
    }

    function commonButtonTypes() {
        return COMMON_BUTTON_TYPE;
    }

    function configTabs() {
        return CONFIG_TABS;
    }

    function commonButtons() {
        return COMMON_BUTTONS;
    }

    return {
        tabTypes: tabTypes,
        commonButtonTypes: commonButtonTypes,
        configTabs: configTabs,
        commonButtons: commonButtons
    }
});