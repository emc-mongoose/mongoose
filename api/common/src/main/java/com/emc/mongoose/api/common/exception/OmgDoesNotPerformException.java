package com.emc.mongoose.api.common.exception;

/**
 Created by kurila on 14.07.16.
 */
public class OmgDoesNotPerformException
extends DanShootHisFootException {

	public OmgDoesNotPerformException(final String msg) {
		super(msg);
	}

	public OmgDoesNotPerformException(final Throwable cause) {
		super(cause);
	}
}
