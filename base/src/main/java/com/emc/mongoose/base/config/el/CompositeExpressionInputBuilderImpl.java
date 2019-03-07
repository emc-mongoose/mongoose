package com.emc.mongoose.base.config.el;

import static com.github.akurilov.commons.io.el.ExpressionInput.EXPRESSION_PATTERN;
import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;

import com.emc.mongoose.base.env.DateUtil;
import com.github.akurilov.commons.io.collection.CompositeStringInput;
import com.github.akurilov.commons.io.el.ExpressionInput;
import com.github.akurilov.commons.io.el.SynchronousExpressionInput;
import com.github.akurilov.commons.math.MathUtil;
import com.github.akurilov.commons.math.Random;
import java.io.File;
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
	public static String format(final String pattern, final Object... args) {
		return FORMATTER.format(pattern, args).toString();
	}

	public static long xor(final long x1, final long x2) {
		return x1 ^ x2;
	}

	private final ExpressionInput.Builder inputBuilder = ExpressionInput.builder();

	private volatile String expr;

	public CompositeExpressionInputBuilderImpl() {
		try {
			function("date", "formatNowIso8601", DateUtil.class.getMethod("formatNowIso8601"));
			function("date", "formatNowRfc1123", DateUtil.class.getMethod("formatNowRfc1123"));
			function("date", "format", DateUtil.class.getMethod("dateFormat", String.class));
			function("date", "from", DateUtil.class.getMethod("date", long.class));
			function("env", "get", System.class.getMethod("getenv", String.class));
			function("int64", "toString", Long.class.getMethod("toString", long.class, int.class));
			function(
							"int64",
							"toUnsignedString",
							Long.class.getMethod("toUnsignedString", long.class, int.class));
			function("int64", "reverse", Long.class.getMethod("reverse", long.class));
			function("int64", "reverseBytes", Long.class.getMethod("reverseBytes", long.class));
			function("int64", "rotateLeft", Long.class.getMethod("rotateLeft", long.class, int.class));
			function("int64", "rotateRight", Long.class.getMethod("rotateRight", long.class, int.class));
			function(
							"int64", "xor", CompositeExpressionInputBuilderImpl.class.getMethod("xor", long.class, long.class));
			function("int64", "xorShift", MathUtil.class.getMethod("xorShift", long.class));
			function("math", "absInt32", Math.class.getMethod("abs", int.class));
			function("math", "absInt64", Math.class.getMethod("abs", long.class));
			function("math", "absFloat32", Math.class.getMethod("abs", float.class));
			function("math", "absFloat64", Math.class.getMethod("abs", double.class));
			function("math", "acos", Math.class.getMethod("acos", double.class));
			function("math", "asin", Math.class.getMethod("asin", double.class));
			function("math", "atan", Math.class.getMethod("atan", double.class));
			function("math", "ceil", Math.class.getMethod("ceil", double.class));
			function("math", "cos", Math.class.getMethod("cos", double.class));
			function("math", "exp", Math.class.getMethod("exp", double.class));
			function("math", "floor", Math.class.getMethod("floor", double.class));
			function("math", "log", Math.class.getMethod("log", double.class));
			function("math", "log10", Math.class.getMethod("log10", double.class));
			function("math", "maxInt32", Math.class.getMethod("max", int.class, int.class));
			function("math", "maxInt64", Math.class.getMethod("max", long.class, long.class));
			function("math", "maxFloat32", Math.class.getMethod("max", float.class, float.class));
			function("math", "maxFloat64", Math.class.getMethod("max", double.class, double.class));
			function("math", "minInt32", Math.class.getMethod("min", int.class, int.class));
			function("math", "minInt64", Math.class.getMethod("min", long.class, long.class));
			function("math", "minFloat32", Math.class.getMethod("min", float.class, float.class));
			function("math", "minFloat64", Math.class.getMethod("min", double.class, double.class));
			function("math", "pow", Math.class.getMethod("pow", double.class, double.class));
			function("math", "sin", Math.class.getMethod("sin", double.class));
			function("math", "sqrt", Math.class.getMethod("sqrt", double.class));
			function("math", "tan", Math.class.getMethod("tan", double.class));
			function("path", "random", RandomPath.class.getMethod("get", int.class, int.class));
			function(
							"string",
							"format",
							CompositeExpressionInputBuilderImpl.class.getMethod(
											"format", new Class[]{String.class, Object[].class
											}));
			function(
							"string",
							"join",
							String.class.getMethod("join", new Class[]{CharSequence.class, CharSequence[].class
							}));
			function("time", "millisSinceEpoch", System.class.getMethod("currentTimeMillis"));
			function("time", "nanos", System.class.getMethod("nanoTime"));
			value("e", Math.E, double.class);
			value("lineSep", System.lineSeparator(), String.class);
			value("pathSep", File.pathSeparator, String.class);
			value("pi", Math.PI, double.class);
			value("rnd", new Random(), Random.class);
		} catch (final NoSuchMethodException e) {
			throwUnchecked(e);
		}
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
		final String prefix, final String name, final Method method
	) {
		inputBuilder.function(prefix, name, method);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final <T extends CompositeExpressionInputBuilder> T value(
		final String name, final Object value, final Class<?> type
	) {
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
				if(!constSegment.isEmpty()) {
					segments.add(constSegment);
					constSegmentBuilder.setLength(0);
				}
				final var inputSegment = inputBuilder
					.expression(fullSegmentExpr)
					.build();
				// the input segment is constant value input if the segment expression is null
				if(null == segmentExpr || inputSegment instanceof SynchronousExpressionInput) {
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
