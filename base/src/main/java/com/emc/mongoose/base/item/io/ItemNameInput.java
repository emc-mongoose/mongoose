package com.emc.mongoose.base.item.io;

import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;

import com.emc.mongoose.base.config.el.AsyncExpressionInput;
import com.emc.mongoose.base.config.el.ExpressionInputBuilder;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.el.ExpressionInput;
import java.util.List;

public final class ItemNameInput implements Input<String> {

  private final ExpressionInput<Long> itemIdInput;
  private final ExpressionInput<String> itemNameInput;

  public ItemNameInput(final String idExpr, final String nameExpr) {
    itemIdInput = ExpressionInputBuilder.newInstance().type(long.class).expression(idExpr).build();
    if (itemIdInput instanceof AsyncExpressionInput) {
      try {
        ((AsyncExpressionInput<Long>) itemIdInput).start();
      } catch (final Exception e) {
        throwUnchecked(e);
      }
    }
    itemNameInput =
        ExpressionInputBuilder.newInstance().type(String.class).expression(nameExpr).build();
    if (itemNameInput instanceof AsyncExpressionInput) {
      try {
        ((AsyncExpressionInput<String>) itemNameInput).start();
      } catch (final Exception e) {
        throwUnchecked(e);
      }
    }
  }

  @Override
  public final String get() {
    return itemNameInput.get();
  }

  @Override
  public final int get(final List<String> buffer, final int limit) {
    return itemNameInput.get(buffer, limit);
  }

  @Override
  public final long skip(final long count) {
    return itemNameInput.skip(count);
  }

  @Override
  public final void reset() {
    itemNameInput.reset();
  }

  @Override
  public final void close() throws Exception {
    itemNameInput.close();
    itemIdInput.close();
  }
}
