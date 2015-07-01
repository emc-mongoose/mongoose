package com.emc.mongoose.core.impl.data;
/**
 Created by andrey on 26.06.15.
 */
public class DataCorruptionException
extends DataVerificationException {
	//
	public final int relOffset;
	public final byte expected, actual;
	//
	public DataCorruptionException(final int relOffset, final byte expected, final byte actual) {
		this.relOffset = relOffset;
		this.expected = expected;
		this.actual = actual;
	}
}
