package com.emc.mongoose.base.data;

/** Created by andrey on 26.06.15. */
public abstract class DataVerificationException extends RuntimeException {

	private final long offset;

	public DataVerificationException(final long offset) {
		this.offset = offset;
	}

	public long getOffset() {
		return offset;
	}
}
