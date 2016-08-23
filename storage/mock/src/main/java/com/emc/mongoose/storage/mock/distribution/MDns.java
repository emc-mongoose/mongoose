package com.emc.mongoose.storage.mock.distribution;

/**
 Created on 23.08.16.
 */
public class MDns {

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
