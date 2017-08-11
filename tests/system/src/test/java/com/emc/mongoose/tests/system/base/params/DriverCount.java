package com.emc.mongoose.tests.system.base.params;

/**
 Created by andrey on 11.08.17.
 */
public enum DriverCount {
	LOCAL(1),
	DISTRIBUTED(2);

	public static final String KEY_ENV = "DRIVER_COUNT";

	private final int value;

	DriverCount(final int value) {
		this.value = value;
	}

	public final int getValue() {
		return value;
	}
}
