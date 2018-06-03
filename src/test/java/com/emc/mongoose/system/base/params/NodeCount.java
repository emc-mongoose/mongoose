package com.emc.mongoose.system.base.params;

/**
 Created by andrey on 11.08.17.
 */
public enum NodeCount {

	LOCAL(1),
	DISTRIBUTED(2);

	public static final String KEY_ENV = "NODE_COUNT";

	private final int value;

	NodeCount(final int value) {
		this.value = value;
	}

	public final int getValue() {
		return value;
	}
}
