/**
 * Created on 01.04.16.
 */
define(["jquery", "handlebars"], function ($, Handlebars) {

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

    function compileAndInsertInside(jQuerySelector, template, paramsMap) {
        const html = compile(template, paramsMap);
        insertInside(jQuerySelector, html);
    }

    function compileAndInsertInsideBefore(jQuerySelector, template, paramsMap) {
        const html = compile(template, paramsMap);
        insertInsideBefore(jQuerySelector, html);
    }

    function compileAndInsertAfter(jQuerySelector, template, paramsMap) {
        const html = compile(template, paramsMap);
        insertAfter(jQuerySelector, html);
    }

    return {
        compileAndInsertInside: compileAndInsertInside,
        compileAndInsertInsideBefore: compileAndInsertInsideBefore,
        compileAndInsertAfter: compileAndInsertAfter
    }
});