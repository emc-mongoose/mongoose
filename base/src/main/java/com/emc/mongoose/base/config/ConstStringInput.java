package com.emc.mongoose.base.config;

import com.github.akurilov.commons.io.Input;
import java.util.List;

public final class ConstStringInput implements Input<String> {

  private String val;

  public ConstStringInput(final String val) {
    this.val = val;
  }

  @Override
  public final String get() {
    return val;
  }

  @Override
  public int get(final List<String> buffer, final int limit) {
    for (var i = 0; i < limit; i++) {
      buffer.add(val);
    }
    return limit;
  }

  @Override
  public final long skip(final long count) {
    return count;
  }

  @Override
  public final void reset() {}

  @Override
  public final void close() {}
}
