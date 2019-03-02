package com.emc.mongoose.base.config.el;

import com.github.akurilov.commons.io.el.ExpressionInput;

public interface ExpressionInputBuilder extends ExpressionInput.Builder {

  String INITIAL_VALUE_EXPRESSION_MARKER = "%";

  static ExpressionInputBuilder newInstance() {
    return new ExpressionInputBuilderImpl();
  }

  static long xor(final long x1, final long x2) {
    return x1 ^ x2;
  }
}
