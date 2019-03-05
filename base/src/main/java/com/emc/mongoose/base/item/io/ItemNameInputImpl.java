package com.emc.mongoose.base.item.io;

import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;

import com.emc.mongoose.base.config.el.AsyncExpressionInput;
import com.emc.mongoose.base.config.el.ExpressionInputBuilder;
import com.github.akurilov.commons.io.el.ExpressionInput;
import java.util.List;

public final class ItemNameInputImpl
				implements ItemNameInput {

	private final ExpressionInput<Long> itemIdInput;
	private final long maxId;
	private final int length;
	private final String prefix;
	private final int radix;

	public ItemNameInputImpl(
					final String idExpr, final int length, final String prefix, final int radix) {
		itemIdInput = ExpressionInputBuilder.newInstance().type(long.class).expression(idExpr).build();
		if (itemIdInput instanceof AsyncExpressionInput) {
			try {
				((AsyncExpressionInput<Long>) itemIdInput).start();
			} catch (final Exception e) {
				throwUnchecked(e);
			}
		}
		this.maxId = (long) Math.pow(radix, length);
		this.length = length;
		this.prefix = prefix;
		this.radix = radix;
	}

	@Override
	public final long lastId() {
		return itemIdInput.last() % maxId;
	}

	@Override
	public final String get() {
		if (prefix == null) {
			return Long.toString(itemIdInput.get() % maxId, radix);
		} else {
			return prefix + Long.toString(itemIdInput.get() % maxId, radix);
		}
	}

	@Override
	public final int get(final List<String> buffer, final int limit) {
		for (var i = 0; i < limit; i++) {
			buffer.add(get());
		}
		return limit;
	}

	@Override
	public final long skip(final long count) {
		return itemIdInput.skip(count);
	}

	@Override
	public final void reset() {
		itemIdInput.reset();
	}

	@Override
	public final void close() throws Exception {
		itemIdInput.close();
	}
}
