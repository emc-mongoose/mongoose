package com.emc.mongoose.base.config;

/** Created by kurila on 14.07.16. */
public class IllegalConfigurationException
	extends IllegalStateException {

	public IllegalConfigurationException() {
		super();
	}

	public IllegalConfigurationException(final String msg) {
		super(msg);
	}

	public IllegalConfigurationException(final Throwable cause) {
		super(cause);
	}

	public IllegalConfigurationException(final String msg, final Throwable cause) {
		super(msg, cause);
	}
}
