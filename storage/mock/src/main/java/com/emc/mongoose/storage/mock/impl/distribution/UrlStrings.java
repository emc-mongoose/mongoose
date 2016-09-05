package com.emc.mongoose.storage.mock.impl.distribution;

/**
 Created on 29.08.16.
 */
public class UrlStrings {

	private static final String PROTOCOL_DELIMITER = "://";
	private static final String PORT_DELIMITER = ":";
	private static final String FILE_DELIMITER = "/";
	private static final ThreadLocal<StringBuilder> SB_TL = new ThreadLocal<StringBuilder>() {
		@Override
		protected StringBuilder initialValue() {
			return new StringBuilder();
		}
	};

	public static String get(final String protocol, final String host,
		final int port, final String file
	) {
		final StringBuilder builder = SB_TL.get();
		builder.setLength(0);
		builder.append(protocol).append(PROTOCOL_DELIMITER).append(host);
		if(port > 0) {
			builder.append(PORT_DELIMITER).append(port);
		}
		if(file != null) {
			builder.append(FILE_DELIMITER).append(file);
		}
		return builder.toString();
	}

	public static String get(final String protocol, final String host, final int port) {
		return get(protocol, host, port, null);
	}

	public static String get(final String protocol, final String host, final String file) {
		return get(protocol, host, 0, file);
	}

	public static String get(final String protocol, final String host) {
		return get(protocol, host, 0, null);
	}

}
