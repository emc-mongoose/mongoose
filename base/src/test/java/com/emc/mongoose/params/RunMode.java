package com.emc.mongoose.params;

/** Created by andrey on 11.08.17. */
public enum RunMode {
	LOCAL(1), DISTRIBUTED(2);

	public static final String KEY_ENV = "RUN_MODE";

	private final int nodeCount;

	RunMode(final int nodeCount) {
		this.nodeCount = nodeCount;
	}

	public final int getNodeCount() {
		return nodeCount;
	}
}
