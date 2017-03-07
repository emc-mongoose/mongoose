package com.emc.mongoose.common.supply;

import java.util.List;

/**
 Created by andrey on 24.10.16.
 */
public final class ConstantStringSupplier
implements BatchSupplier<String> {

	protected final String value;

	public ConstantStringSupplier(final String value) {
		this.value = value;
	}

	@Override
	public final String get() {
		return value;
	}

	@Override
	public final int get(final List<String> buffer, final int limit) {
		for(int i = 0; i < limit; i ++) {
			buffer.add(value);
		}
		return limit;
	}

	@Override
	public final long skip(final long count) {
		return count;
	}

	@Override
	public final void reset() {
	}

	@Override
	public final void close() {
	}
}
