package com.emc.mongoose.tests.system.base.params;

import com.emc.mongoose.api.common.SizeInBytes;

/**
 Created by andrey on 11.08.17.
 */
public enum ItemSize {
	EMPTY(new SizeInBytes(0)),
	SMALL(new SizeInBytes("10KB")),
	MEDIUM(new SizeInBytes("1MB")),
	LARGE(new SizeInBytes("100MB")),
	HUGE(new SizeInBytes("10GB"));

	public static final String KEY_ENV = "ITEM_SIZE";

	private final SizeInBytes value;

	ItemSize(final SizeInBytes value) {
		this.value = value;
	}

	@Override
	public final String toString() {
		return name();
	}

	public final SizeInBytes getValue() {
		return value;
	}
}
