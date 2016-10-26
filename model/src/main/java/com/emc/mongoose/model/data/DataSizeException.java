package com.emc.mongoose.model.data;

/**
 Created by andrey on 26.06.15.
 */
public class DataSizeException
extends DataVerificationException {
	
	private final long expected;
	
	public DataSizeException(final long expected, final long actual) {
		super(actual);
		this.expected = expected;
	}
	
	public long getExpected() {
		return expected;
	}
}
