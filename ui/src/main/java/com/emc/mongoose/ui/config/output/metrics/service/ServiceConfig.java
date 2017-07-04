package com.emc.mongoose.ui.config.output.metrics.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class ServiceConfig
implements Serializable {

	public static final String KEY_ENABLED = "enabled";
	public static final String KEY_PORT = "port";

	public final void setEnabled(final boolean enabledFlag) {
		this.enabledFlag = enabledFlag;
	}

	public final void setPort(final int port) {
		this.port = port;
	}

	@JsonProperty(KEY_ENABLED) private boolean enabledFlag;
	@JsonProperty(KEY_PORT) private int port;

	public ServiceConfig() {
	}

	public ServiceConfig(final ServiceConfig other) {
		this.enabledFlag = other.getEnabled();
		this.port = other.getPort();
	}

	public final boolean getEnabled() {
		return enabledFlag;
	}

	public final int getPort() {
		return port;
	}
}