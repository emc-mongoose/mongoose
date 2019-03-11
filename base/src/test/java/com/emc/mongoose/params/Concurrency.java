package com.emc.mongoose.params;

/** Created by andrey on 11.08.17. */
public enum Concurrency {
	UNLIMITED(0), SINGLE(1), LOW(10), MEDIUM(100), HIGH(1000);

	public static final String KEY_ENV = "CONCURRENCY";

	private final int value;

	Concurrency(final int value) {
		this.value = value;
	}

	public final int getValue() {
		return value;
	}
}
