package com.emc.mongoose.base.config.el;

import com.github.akurilov.commons.io.el.ExpressionInput;

public interface ExpressionInputBuilder
extends ExpressionInput.Builder {

	static ExpressionInputBuilder newInstance() {
		return new ExpressionInputBuilderImpl();
	}
}
