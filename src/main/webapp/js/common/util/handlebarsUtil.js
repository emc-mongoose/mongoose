/**
 * Created on 01.04.16.
 */
define(['jquery', 'handlebars'], function ($, Handlebars) {

    function compile(template, paramsObj) {
        const compiled = Handlebars.compile(template);
        if (paramsObj) {
            return compiled(paramsObj);
        } else {
            return compiled();
        }
    }

    function insertInside(jqSelector, html) {
        $(jqSelector).append(html);
    }

    function insertInsideBefore(jqSelector, html) {
        $(jqSelector).prepend(html);
    }

    function insertAfter(jqSelector, html) {
        $(jqSelector).after(html);
    }

    function compileAndInsertBase(jQuerySelector, template, paramsMap, insertFunc) {
        const html = compile(template, paramsMap);
        insertFunc(jQuerySelector, html);
    }
    
    function compileAndInsertInside(jQuerySelector, template, paramsMap) {
        compileAndInsertBase(jQuerySelector, template, paramsMap, insertInside);
    }

    function compileAndInsertInsideBefore(jQuerySelector, template, paramsMap) {
        compileAndInsertBase(jQuerySelector, template, paramsMap, insertInsideBefore);
    }

    function compileAndInsertAfter(jQuerySelector, template, paramsMap) {
        compileAndInsertBase(jQuerySelector, template, paramsMap, insertAfter);

    }

    return {
        compileAndInsertInside: compileAndInsertInside,
        compileAndInsertInsideBefore: compileAndInsertInsideBefore,
        compileAndInsertAfter: compileAndInsertAfter
    }
});