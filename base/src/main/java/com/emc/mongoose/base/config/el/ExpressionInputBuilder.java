package com.emc.mongoose.base.config.el;

import com.github.akurilov.commons.io.el.ExpressionInput;

import java.util.regex.Pattern;

import static com.github.akurilov.commons.io.el.ExpressionInput.ASYNC_MARKER;
import static com.github.akurilov.commons.io.el.ExpressionInput.SYNC_MARKER;

public interface ExpressionInputBuilder extends ExpressionInput.Builder {

	Pattern EXPRESSION_PATTERN = Pattern.compile("(?<expr>[$#]\\{[^}]+})?(?<init>%\\{[^}]+})?");

	String ASYNC_EXPR_START_MARKER = ASYNC_MARKER + '{';
	String SYNC_EXPR_START_MARKER = SYNC_MARKER + '{';

	static ExpressionInputBuilder newInstance() {
		return new ExpressionInputBuilderImpl();
	}

	static long xor(final long x1, final long x2) {
		return x1 ^ x2;
	}
}
