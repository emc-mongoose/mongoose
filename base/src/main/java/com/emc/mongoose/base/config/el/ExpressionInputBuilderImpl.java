package com.emc.mongoose.base.config.el;

import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;

import com.github.akurilov.commons.io.el.ExpressionInput;
import com.github.akurilov.commons.io.el.SynchronousExpressionInput;
import com.github.akurilov.commons.math.MathUtil;
import java.io.File;

public class ExpressionInputBuilderImpl
    extends com.github.akurilov.commons.io.el.ExpressionInputBuilder
    implements ExpressionInputBuilder {

  public ExpressionInputBuilderImpl() {
    try {
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
      function("math", "xorShift64", MathUtil.class.getMethod("xorShift", long.class));
      function(
          "string",
          "format",
          String.class.getMethod("format", new Class[] {String.class, Object[].class}));
      function(
          "string",
          "join",
          String.class.getMethod("join", new Class[] {CharSequence.class, CharSequence[].class}));
      function("time", "millisSinceEpoch", System.class.getMethod("currentTimeMillis"));
      function("time", "nanos", System.class.getMethod("nanoTime"));
      value("e", Math.E, double.class);
      value("lineSep", System.lineSeparator(), String.class);
      value("pathSep", File.pathSeparator, String.class);
      value("pi", Math.PI, double.class);
    } catch (final NoSuchMethodException e) {
      throwUnchecked(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T, U extends ExpressionInput<T>> U build() {
    var input = super.<T, U>build();
    if (!(input instanceof SynchronousExpressionInput)) {
      input =
          (U)
              new AsyncExpressionInputImpl<>(
                  input); // async input case, wrap with the refreshing fiber
    }
    return input;
  }
}
