package com.emc.mongoose.common.exception;

/**
 Created by kurila on 14.07.16.
 */
public class Fireball
extends Exception {

	public Fireball() {
	}

	public Fireball(final String msg) {
		super(msg);
	}

	public Fireball(final Throwable cause) {
		super(cause);
	}
}
