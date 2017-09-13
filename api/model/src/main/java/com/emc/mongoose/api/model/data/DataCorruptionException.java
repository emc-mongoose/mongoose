package com.emc.mongoose.api.model.data;

/**
 Created by andrey on 26.06.15.
 */
public class DataCorruptionException
extends DataVerificationException {
	//
	public final byte expected, actual;
	//
	public DataCorruptionException(final long relOffset, final byte expected, final byte actual) {
		super(relOffset);
		this.expected = expected;
		this.actual = actual;
	}
}
