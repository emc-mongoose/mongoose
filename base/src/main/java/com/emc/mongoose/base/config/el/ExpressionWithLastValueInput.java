package com.emc.mongoose.base.config.el;

import com.github.akurilov.commons.io.el.ExpressionInput;
import com.github.akurilov.commons.math.MathUtil;

import java.util.function.Function;

public class ExpressionWithLastValueInput<T>
implements ExpressionInput<T> {

	private volatile T last;

	public ExpressionWithLastValueInput(final T initial) {
		this.last = initial;
	}
	void f() {
		final var input = ExpressionInput.<Long>builder()
			.value("last", last, last.getClass())
			.func("long", "xorShift", MathUtil.class.getMethod("xorShift", long.class))
			.expr("${long:xorshift}")
			.type()
	}
}
