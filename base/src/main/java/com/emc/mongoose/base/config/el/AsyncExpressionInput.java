package com.emc.mongoose.base.config.el;

import com.github.akurilov.commons.io.el.ExpressionInput;
import com.github.akurilov.fiber4j.Fiber;

public interface AsyncExpressionInput<T> extends ExpressionInput<T>, Fiber {

	/**
	* @return last value, without re-evaluation
	*/
	@Override
	T call();
}
