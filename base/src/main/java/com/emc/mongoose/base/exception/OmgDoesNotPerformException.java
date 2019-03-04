package com.emc.mongoose.base.exception;

/** Created by kurila on 14.07.16. */
public class OmgDoesNotPerformException extends OmgShootMyFootException {

	public OmgDoesNotPerformException(final String msg) {
		super(msg);
	}

	public OmgDoesNotPerformException(final Throwable cause) {
		super(cause);
	}
}
