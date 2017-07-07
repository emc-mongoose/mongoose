package com.emc.mongoose.ui.config.storage.net.node;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 Created by andrey on 05.07.17.
 */
public final class NodeConfig
implements Serializable {

	public static final String KEY_ADDRS = "addrs";
	public static final String KEY_PORT = "port";
	public static final String KEY_CONN_ATTEMPTS_LIMIT = "connAttemptsLimit";

	public final void setAddrs(final List<String> addrs) {
		this.addrs = addrs;
	}

	public final void setPort(final int port) {
		this.port = port;
	}

	public final void setConnAttemptsLimit(final int connAttemtsLimit) {
		this.connAttemptsLimit = connAttemtsLimit;
	}

	@JsonProperty(KEY_ADDRS) private List<String> addrs;
	@JsonProperty(KEY_PORT) private int port;
	@JsonProperty(KEY_CONN_ATTEMPTS_LIMIT) private int connAttemptsLimit;

	public NodeConfig() {
	}

	public NodeConfig(final NodeConfig other) {
		this.addrs = new ArrayList<>(other.getAddrs());
		this.port = other.getPort();
		this.connAttemptsLimit = other.getConnAttemptsLimit();
	}

	public final List<String> getAddrs() {
		return addrs;
	}

	public final int getPort() {
		return port;
	}

	public final int getConnAttemptsLimit() {
		return connAttemptsLimit;
	}
}