/**
 * Created on 01.04.16.
 */
define(function() {

    const MODE = {
        STANDALONE: 'standalone',
        CLIENT: 'client',
        WSMOCK: 'wsmock',
        SERVER: 'server'
    };

    // the order of elements matters for a template
    const TAB_TYPE = {
        SCENARIOS: 'scenarios',
        DEFAULTS: 'defaults',
        TESTS: 'tests'
    };

    // the order of elements matters for a template
    const TESTS_TAB_TYPE = {
        LIST: 'list',
        LOGS: 'logs',
        CHARTS: 'charts'
    };

    // the order of elements matters for a template
    const TESTS_CHARTS_TAB_TYPE = {
        DURATION: 'duration',
        LATENCY: 'latency',
        THROUGHPUT: 'throughput',
        BANDWIDTH: 'bandwidth'
    };

    // the order of elements matters for a template
    const TESTS_LOGS_TAB_TYPE = {
        MESSAGES: 'messages',
        ERRORS: 'errors',
        PERFAVG: 'perf\.avg',
        PERFSUM: 'perf\.sum'
    };

    // the order of elements matters for a template
    const COMMON_BUTTON_TYPE = {
        OPEN: 'open',
        OPEN_INPUT_TEXT: 'file-name',
        OPEN_INPUT_FILE: 'file',
        SAVE: 'save',
        SAVE_AS: 'save-file'
    };
    
    const CONFIG_TREE_ELEMENT = {
        TREE: 'folders',
        NODE: 'dir',
        LEAF: 'file'
    };

    const TAB_CLASS = {
        ACTIVE: 'active',
        SELECTED: 'ui-selected'
    };

    const BLOCK = {
        BUTTONS: 'buttons',
        TREE: CONFIG_TREE_ELEMENT.TREE,
        CONFIG: 'configurations'
    };

    const CHART_TYPE = {
        CURRENT: 'current',
        TOTAL: 'total'
    };

    const DELIMITER = {
        ID: '-',
        PATH: '/',
        PROPERTY: '.'
    };

    function modes() {
        return MODE;
    }

    function tabTypes() {
        return TAB_TYPE;
    }

    function chartTypes() {
        return CHART_TYPE;
    }

    function testsTabTypes() {
        return TESTS_TAB_TYPE;
    }

    function testsChartsTabTypes() {
        return TESTS_CHARTS_TAB_TYPE;
    }

    function testsLogsTabTypes() {
        return TESTS_LOGS_TAB_TYPE;
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

    function composeId(partsArr) {
	    return partsArr.join(DELIMITER.ID);
    }
    
    function composeJqueryId(partsArr) {
        return '#' + composeId(partsArr);
    }

    function getComposedJquerySelector(partsArr) {
        return $(composeJqueryId(partsArr));
    }

    function objPartToArray(obj, numberOfElems) {
        return objToArray(obj).slice(0, numberOfElems);
    }

    function objToArray(obj) {
        return $.map(obj, function (value) {
            return value;
        });
    }

    return {
        modes: modes,
        tabTypes: tabTypes,
        testsTabTypes: testsTabTypes,
        testsLogsTabTypes: testsLogsTabTypes,
        testsChartsTabTypes: testsChartsTabTypes,
        chartTypes: chartTypes,
        commonButtonTypes: commonButtonTypes,
        configTreeElements: configTreeElements,
        tabClasses: tabClasses,
        blocks: blocks,
        delimiters: delimiters,
        composeId: composeId,
        composeJqId:composeJqueryId,
        getComposedJqSelector: getComposedJquerySelector,
        objToArray: objToArray,
        objPartToArray: objPartToArray
    }
});