package com.emc.mongoose.ui.config.load.batch;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class BatchConfig
implements Serializable {

	public static final String KEY_SIZE = "size";

	@JsonProperty(KEY_SIZE) private int size;

	public BatchConfig() {
	}

	public BatchConfig(final BatchConfig other) {
		this.size = other.size;
	}

	public final void setSize(final int size) {
		this.size = size;
	}

	public final int getSize() {
		return size;
	}
}