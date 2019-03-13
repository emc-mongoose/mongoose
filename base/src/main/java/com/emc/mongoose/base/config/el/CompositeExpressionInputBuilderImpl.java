package com.emc.mongoose.base.config.el;

import static com.emc.mongoose.base.config.el.Language.withLanguage;
import static com.github.akurilov.commons.io.el.ExpressionInput.EXPRESSION_PATTERN;

import com.github.akurilov.commons.io.collection.CompositeStringInput;
import com.github.akurilov.commons.io.el.ExpressionInput;
import com.github.akurilov.commons.io.el.SynchronousExpressionInput;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class CompositeExpressionInputBuilderImpl
				implements CompositeExpressionInputBuilder {

	static {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}
	static final Pattern INITIAL_VALUE_PATTERN = Pattern.compile(".*(%\\{.+})([$#]\\{.+}.)*");
	static final Formatter FORMATTER = new Formatter(Locale.ROOT);
	private final ExpressionInput.Builder inputBuilder = ExpressionInput.builder();

	private volatile String expr;

	public CompositeExpressionInputBuilderImpl() {
		withLanguage(inputBuilder);
	}

	@SuppressWarnings("unchecked")
	@Override
	public final <T extends CompositeExpressionInputBuilder> T expression(final String expr) {
		this.expr = expr;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final <T extends CompositeExpressionInputBuilder> T function(
					final String prefix, final String name, final Method method) {
		inputBuilder.function(prefix, name, method);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final <T extends CompositeExpressionInputBuilder> T value(
					final String name, final Object value, final Class<?> type) {
		inputBuilder.value(name, value, type);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final CompositeStringInput build() {
		final var segments = (List) new ArrayList();
		final var matcher = EXPRESSION_PATTERN.matcher(expr);
		final var constSegmentBuilder = new StringBuilder();
		var start = 0;
		var end = 0;
		while (matcher.find()) {
			end = matcher.start();
			if (end > 0) {
				constSegmentBuilder.append(expr, start, end);
			}
			start = matcher.end();
			final var fullSegmentExpr = matcher.group(0);
			final var segmentExpr = matcher.group(1);
			final var segmentInit = matcher.group(2);
			if (segmentExpr != null || segmentInit != null) {
				final var constSegment = constSegmentBuilder.toString();
				if (!constSegment.isEmpty()) {
					segments.add(constSegment);
					constSegmentBuilder.setLength(0);
				}
				final var inputSegment = inputBuilder
								.expression(fullSegmentExpr)
								.build();
				// the input segment is constant value input if the segment expression is null
				if (null == segmentExpr || inputSegment instanceof SynchronousExpressionInput) {
					segments.add(inputSegment);
				} else {
					final var asyncInputSegment = new AsyncExpressionInputImpl<>(inputSegment);
					asyncInputSegment.start();
					segments.add(asyncInputSegment);
				}
			}
		}
		segments.add(constSegmentBuilder.toString());
		return new CompositeStringInput(segments.toArray());
	}
}
