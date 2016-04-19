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


    const DELIMITER = {
        ID: '-',
        PATH: '/',
        PROPERTY: '.'
    };

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

    function delimiters() {
        return DELIMITER;
    }

    String.prototype.replaceAll = function(search, replacement) {
        var target = this;
        return target.replace(new RegExp(search, 'g'), replacement);
    };

    function composeId(partsArr) {
	    return partsArr.join(DELIMITER.ID);
    }
    
    function composeJqueryId(partsArr) {
        return '#' + composeId(partsArr);
    }

    function getComposedJquerySelector(partsArr) {
        return $(composeJqueryId(partsArr));
    }

    return {
        tabTypes: tabTypes,
        commonButtonTypes: commonButtonTypes,
        configTreeElements: configTreeElements,
        tabClasses: tabClasses,
        blocks: blocks,
        delimiters: delimiters,
        composeId: composeId,
        composeJqId:composeJqueryId,
        getComposedJqSelector: getComposedJquerySelector
    }
});