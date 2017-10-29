package com.emc.mongoose.ui.config.storage.mock;

import com.emc.mongoose.ui.config.storage.mock.container.ContainerConfig;
import com.emc.mongoose.ui.config.storage.mock.fail.FailConfig;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class MockConfig
implements Serializable {

	public static final String KEY_CAPACITY = "capacity";
	public static final String KEY_CONTAINER = "container";
	public static final String KEY_FAIL = "fail";
	public static final String KEY_NODE = "node";

	public final void setCapacity(final int capacity) {
		this.capacity = capacity;
	}

	public final void setContainerConfig(final ContainerConfig containerConfig) {
		this.containerConfig = containerConfig;
	}

	public final void setFailConfig(final FailConfig failConfig) {
		this.failConfig = failConfig;
	}

	public final void setNode(final boolean node) {
		this.node = node;
	}

	@JsonProperty(KEY_CAPACITY) private int capacity;
	@JsonProperty(KEY_CONTAINER) private ContainerConfig containerConfig;
	@JsonProperty(KEY_FAIL) private FailConfig failConfig;
	@JsonProperty(KEY_NODE) private boolean node;

	public MockConfig() {
	}

	public MockConfig(final MockConfig other) {
		this.capacity = other.getCapacity();
		this.containerConfig = new ContainerConfig(other.getContainerConfig());
		this.failConfig = new FailConfig(other.getFailConfig());
		this.node = other.getNode();
	}

	public int getCapacity() {
		return capacity;
	}

	public ContainerConfig getContainerConfig() {
		return containerConfig;
	}

	public FailConfig getFailConfig() {
		return failConfig;
	}

	public boolean getNode() {
		return node;
	}
}