package com.emc.mongoose.base.config;

import java.util.List;

public final class ConstantValueInputImpl<T> implements ConstantValueInput<T> {

	private final T val;

	public ConstantValueInputImpl(final T val) {
		this.val = val;
	}

	@Override
	public final T get() {
		return val;
	}

	@Override
	public int get(final List<T> buffer, final int limit) {
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
