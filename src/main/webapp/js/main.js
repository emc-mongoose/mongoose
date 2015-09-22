/**
 * Created by gusakk on 22.09.15.
 */
//
jQuery.fn.addChild = function(html) {
	var target = $(this[0]);
	var child = $(html);
	child.appendTo(target);
	return child;
};
//
jQuery.fn.prependChild = function(html) {
	var target = $(this[0]);
	var child = $(html);
	child.prependTo(target);
	return child;
};
//
(function($) {
	$.strRemove = function(theTarget, theString) {
		return $("<div/>").append(
			$(theTarget, theString).remove().end()
		).html();
	};
})(jQuery);