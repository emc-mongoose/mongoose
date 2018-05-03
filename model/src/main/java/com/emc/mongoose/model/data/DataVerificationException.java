package com.emc.mongoose.model.data;

import java.io.IOException;

/**
 Created by andrey on 26.06.15.
 */
public abstract class DataVerificationException
extends IOException {

	private final long offset;
	
	public DataVerificationException(final long offset) {
		this.offset = offset;
	}

	public long getOffset() {
		return offset;
	}
}
