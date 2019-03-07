package com.emc.mongoose.base.item.naming;

import it.unimi.dsi.fastutil.longs.Long2LongFunction;

import java.util.List;

public final class ItemNameInputImpl
				implements ItemNameInput {

	private final long initialId;
	private final Long2LongFunction idFunction;
	private volatile long lastId;
	private final String prefix;
	private final int radix;

	public ItemNameInputImpl(
					final Long2LongFunction idFunction, final long offset, final String prefix, final int radix) {
		this.initialId = offset;
		this.idFunction = idFunction;
		this.prefix = prefix;
		this.radix = radix;
	}

	@Override
	public final long lastId() {
		return lastId;
	}

	@Override
	public final String get() {
		if (prefix == null) {
			return Long.toString(idFunction.applyAsLong(lastId), radix);
		} else {
			return prefix + Long.toString(idFunction.applyAsLong(lastId), radix);
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
		for (var i = 0L; i < count; i++) {
			idFunction.applyAsLong(lastId);
		}
		return count;
	}

	@Override
	public final void reset() {
		lastId = initialId;
	}

	@Override
	public final void close() {}
}