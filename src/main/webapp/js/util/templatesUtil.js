/**
 * Created on 01.04.16.
 */
define(function() {
    
    // the order of elements matters for a template
    const TAB_TYPE = {
        SCENARIOS: 'scenarios',
        DEFAULTS: 'defaults',
        TESTS: 'tests'
    };

    // the order of elements matters for a template
    const COMMON_BUTTON_TYPE = {
        OPEN: 'open',
        OPEN_INPUT_TEXT: 'config-file-name',
        OPEN_INPUT_FILE: 'config-file',
        SAVE: 'save-config',
        SAVE_AS: 'save-file'
    };
    
    const CONFIG_TREE_ELEMENT = {
        TREE: 'folders',
        NODE: 'dir',
        LEAF: 'file'
    };

    const TAB_CLASS = {
        ACTIVE: 'active'
    };

    const BLOCK = {
        BUTTONS: 'buttons',
        TREE: CONFIG_TREE_ELEMENT.TREE
    };

    const DELIMITER = '-';

    function tabTypes() {
        return TAB_TYPE;
    }

    function commonButtonTypes() {
        return COMMON_BUTTON_TYPE;
    }

    function configTreeElements() {
        return CONFIG_TREE_ELEMENT;
    }

    function tabClasses() {
        return TAB_CLASS;
    }

    function blocks() {
        return BLOCK;
    }

    String.prototype.replaceAll = function(search, replacement) {
        var target = this;
        return target.replace(new RegExp(search, 'g'), replacement);
    };

    function composeId(partsArr) {
	    return partsArr.join(DELIMITER);
    }
    
    function composeJqueryId(partsArr) {
        return '#' + composeId(partsArr);
    }

    function getComposedJquerySelector(partsArr) {
        $(composeJqueryId(partsArr));
    }

    return {
        tabTypes: tabTypes,
        commonButtonTypes: commonButtonTypes,
        configTreeElements: configTreeElements,
        tabClasses: tabClasses,
        blocks: blocks,
        composeId: composeId,
        composeJqId:composeJqueryId,
        getComposedJqSelector: getComposedJquerySelector
    }
});