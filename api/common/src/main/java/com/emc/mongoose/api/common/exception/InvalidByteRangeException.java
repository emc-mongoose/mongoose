package com.emc.mongoose.api.common.exception;

/**
 Created by kurila on 26.09.16.
 */
public final class InvalidByteRangeException
extends IllegalArgumentException {
	
	public InvalidByteRangeException(final String msg) {
		super(msg);
	}
}
