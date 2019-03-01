package com.emc.mongoose.base.config.el;

import com.github.akurilov.commons.io.el.ExpressionInput;
import com.github.akurilov.fiber4j.Fiber;

public interface AsyncExpressionInput<T> extends ExpressionInput<T>, Fiber {

  /**
   * The method does nothing. It's disabled in order to protect the last value from concurrent
   * update (not safe).
   *
   * @return null
   */
  @Override
  T call();
}
