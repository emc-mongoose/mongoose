package com.emc.mongoose.ui.config.storage.mock.container;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class ContainerConfig
implements Serializable {

	public static final String KEY_CAPACITY = "capacity";
	public static final String KEY_COUNT_LIMIT = "countLimit";

	public final void setCapacity(final int capacity) {
		this.capacity = capacity;
	}

	public final void setCountLimit(final int countLimit) {
		this.countLimit = countLimit;
	}

	@JsonProperty(KEY_CAPACITY) private int capacity;
	@JsonProperty(KEY_COUNT_LIMIT) private int countLimit;

	public ContainerConfig() {
	}

	public ContainerConfig(final ContainerConfig other) {
		this.capacity = other.getCapacity();
		this.countLimit = other.getCountLimit();
	}

	public int getCapacity() {
		return capacity;
	}

	public int getCountLimit() {
		return countLimit;
	}
}