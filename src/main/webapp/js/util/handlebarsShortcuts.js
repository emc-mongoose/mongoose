/**
 * Created on 01.04.16.
 */
define(["jquery", "handlebars"], function ($, Handlebars) {
    function compileAndInsert(existingElementId, position, template, paramsMap) {
        const html = compile(template, paramsMap);
        insert(existingElementId, position, html);
    }

    function compile(template, paramsMap) {
        const compiled = Handlebars.compile(template);
        return compiled(paramsMap);
    }

    function insert(existingElementId, position, html) {
        document.getElementById(existingElementId).insertAdjacentHTML(position, html);
    }

    return {
        compile: compile,
        insert: insert,
        compileAndInsert: compileAndInsert
    }
});