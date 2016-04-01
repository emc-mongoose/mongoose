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
        OPEN_INPUT_FILE: 'config-file',
        OPEN_INPUT_TEXT: 'config-file-name',
        SAVE: 'save-config',
        SAVE_AS: 'save-file'
    };

    const CONFIG_TABS = [TAB_TYPE.SCENARIOS, TAB_TYPE.DEFAULTS];

    function tabTypes() {
        return TAB_TYPE;
    }

    function commonButtonTypes() {
        return COMMON_BUTTON_TYPE;
    }

    function configTabs() {
        return CONFIG_TABS;
    }

    return {
        tabTypes: tabTypes,
        commonButtonTypes: commonButtonTypes,
        configTabs: configTabs
    }
});