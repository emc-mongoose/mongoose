package com.emc.mongoose.ui.config.storage.mock.fail;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class FailConfig
implements Serializable {

	public static final String KEY_CONNECTIONS = "connections";
	public static final String KEY_RESPONSES = "responses";
	@JsonProperty(KEY_CONNECTIONS) private long connections;
	@JsonProperty(KEY_RESPONSES) private long responses;

	public FailConfig() {
	}

	public FailConfig(final FailConfig other) {
		this.connections = other.getConnections();
		this.responses = other.getResponses();
	}

	public final long getConnections() {
		return connections;
	}

	public final void setConnections(final long connections) {
		this.connections = connections;
	}

	public final long getResponses() {
		return responses;
	}

	public final void setResponses(final long responses) {
		this.responses = responses;
	}
}