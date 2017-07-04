package com.emc.mongoose.ui.config.load.queue;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class QueueConfig
implements Serializable {

	public static final String KEY_SIZE = "size";

	public final void setSize(final int size) {
		this.size = size;
	}

	@JsonProperty(KEY_SIZE) private int size;

	public QueueConfig() {
	}

	public QueueConfig(final QueueConfig other) {
		this.size = other.getSize();
	}

	public final int getSize() {
		return size;
	}
}