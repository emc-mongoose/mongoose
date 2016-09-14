package com.emc.mongoose.storage.mock.impl.remote;

/**
 Created on 23.08.16.
 */
public class MDns {

	public static final int DEFAULT_PORT = 9019;

	private static final String PREFIX = "_";
	private static final String SUFFIX = "._tcp.local.";

	private static String collectFullName(final String name) {
		return PREFIX + name + SUFFIX;
	}

	public enum Type {
		WORKSTATION {
			@Override
			public String toString() {
				return collectFullName("workstation");
			}
		},
		HTTP {
			@Override
			public String toString() {
				return collectFullName("http");
			}
		}
	}
}
