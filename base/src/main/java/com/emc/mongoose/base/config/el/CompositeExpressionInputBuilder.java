package com.emc.mongoose.base.config.el;

import com.github.akurilov.commons.io.collection.CompositeStringInput;

import java.lang.reflect.Method;

public interface CompositeExpressionInputBuilder {

	<T extends CompositeExpressionInputBuilder> T expression(final String expr);

	<T extends CompositeExpressionInputBuilder> T function(final String prefix, final String name, final Method method);

	<T extends CompositeExpressionInputBuilder> T value(final String name, final Object value, final Class<?> type);

	CompositeStringInput build();

	static CompositeExpressionInputBuilder newInstance() {
		return new CompositeExpressionInputBuilderImpl();
	}
}
