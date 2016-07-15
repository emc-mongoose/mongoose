package com.emc.mongoose.common.exception;

import java.io.IOException;

/**
 Created by kurila on 15.07.16.
 */
public class IoFireball
extends IOException {

	public IoFireball() {
		super();
	}

	public IoFireball(final String msg) {
		super(msg);
	}

	public IoFireball(final IOException e) {
		super(e.getCause());
	}
}
