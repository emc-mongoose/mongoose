package com.emc.mongoose.core.api.item.data;
/**
 Created by andrey on 26.06.15.
 */
public class DataCorruptionException
extends DataVerificationException {
	//
	public final byte expected, actual;
	//
	public DataCorruptionException(final int relOffset, final byte expected, final byte actual) {
		super.offset = relOffset;
		this.expected = expected;
		this.actual = actual;
	}
}
