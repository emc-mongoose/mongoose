package com.emc.mongoose.base.config.el;

import com.emc.mongoose.base.env.DateUtil;
import com.github.akurilov.commons.io.el.ExpressionInput;
import com.github.akurilov.commons.math.MathUtil;
import com.github.akurilov.commons.math.Random;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public interface Language {

	String FUNC_NAME_SEPARATOR = ":";

	Map<String, Method> FUNCTIONS = new HashMap<>() {
		{
			try {
				put("date:formatNowIso8601", DateUtil.class.getMethod("formatNowIso8601"));
				put("date:formatNowRfc1123", DateUtil.class.getMethod("formatNowRfc1123"));
				put("date:format", DateUtil.class.getMethod("dateFormat", String.class));
				put("date:from", DateUtil.class.getMethod("date", long.class));
				put("env:get", System.class.getMethod("getenv", String.class));
				put("int64:toString", Long.class.getMethod("toString", long.class, int.class));
				put("int64:toUnsignedString", Long.class.getMethod("toUnsignedString", long.class, int.class));
				put("int64:reverse", Long.class.getMethod("reverse", long.class));
				put("int64:reverseBytes", Long.class.getMethod("reverseBytes", long.class));
				put("int64:rotateLeft", Long.class.getMethod("rotateLeft", long.class, int.class));
				put("int64:rotateRight", Long.class.getMethod("rotateRight", long.class, int.class));
				put("int64:xor", Language.class.getMethod("xor", long.class, long.class));
				put("int64:xorShift", MathUtil.class.getMethod("xorShift", long.class));
				put("math:absInt32", Math.class.getMethod("abs", int.class));
				put("math:absInt64", Math.class.getMethod("abs", long.class));
				put("math:absFloat32", Math.class.getMethod("abs", float.class));
				put("math:absFloat64", Math.class.getMethod("abs", double.class));
				put("math:acos", Math.class.getMethod("acos", double.class));
				put("math:asin", Math.class.getMethod("asin", double.class));
				put("math:atan", Math.class.getMethod("atan", double.class));
				put("math:ceil", Math.class.getMethod("ceil", double.class));
				put("math:cos", Math.class.getMethod("cos", double.class));
				put("math:exp", Math.class.getMethod("exp", double.class));
				put("math:floor", Math.class.getMethod("floor", double.class));
				put("math:log", Math.class.getMethod("log", double.class));
				put("math:log10", Math.class.getMethod("log10", double.class));
				put("math:maxInt32", Math.class.getMethod("max", int.class, int.class));
				put("math:maxInt64", Math.class.getMethod("max", long.class, long.class));
				put("math:maxFloat32", Math.class.getMethod("max", float.class, float.class));
				put("math:maxFloat64", Math.class.getMethod("max", double.class, double.class));
				put("math:minInt32", Math.class.getMethod("min", int.class, int.class));
				put("math:minInt64", Math.class.getMethod("min", long.class, long.class));
				put("math:minFloat32", Math.class.getMethod("min", float.class, float.class));
				put("math:minFloat64", Math.class.getMethod("min", double.class, double.class));
				put("math:pow", Math.class.getMethod("pow", double.class, double.class));
				put("math:sin", Math.class.getMethod("sin", double.class));
				put("math:sqrt", Math.class.getMethod("sqrt", double.class));
				put("math:tan", Math.class.getMethod("tan", double.class));
				put("path:random", RandomPath.class.getMethod("get", int.class, int.class));
				put("string:format",
					Language.class.getMethod(
						"format", new Class[]{String.class, Object[].class
						}));
				put(
					"string:join",
					String.class.getMethod("join", new Class[]{CharSequence.class, CharSequence[].class
					}));
				put("time:millisSinceEpoch", System.class.getMethod("currentTimeMillis"));
				put("time:nanos", System.class.getMethod("nanoTime"));
			} catch(final NoSuchMethodException e) {
				throw new AssertionError(e);
			}
		}
	};

	static String format(final String pattern, final Object... args) {
		return CompositeExpressionInputBuilderImpl.FORMATTER.format(pattern, args).toString();
	}

	static long xor(final long x1, final long x2) {
		return x1 ^ x2;
	}

	final class Value {
		final Object value;
		final Class type;
		Value(final Object value, final Class type) {
			this.value = value;
			this.type = type;
		}
	}

	Map<String, Value> VALUES = new HashMap<>() {{
		put("e", new Value(Math.E, double.class));
		put("lineSep", new Value(System.lineSeparator(), String.class));
		put("pathSep", new Value(File.pathSeparator, String.class));
		put("pi", new Value(Math.PI, double.class));
		put("rnd", new Value(new Random(), Random.class));
	}};

	static ExpressionInput.Builder withLanguage(final ExpressionInput.Builder builder) {
		for(final var func: Language.FUNCTIONS.entrySet()) {
			final var name = func.getKey().split(Language.FUNC_NAME_SEPARATOR);
			final var method = func.getValue();
			builder.function(name[0], name[1], method);
		}
		for(final var val: Language.VALUES.entrySet()) {
			final var name = val.getKey();
			final var v = val.getValue();
			builder.value(name, v.value, v.type);
		}
		return builder;
	}
}
