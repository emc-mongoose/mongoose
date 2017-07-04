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

	public final void setAddrs(final List<String> addrs) {
		this.addrs = addrs;
	}

	public final void setPort(final int port) {
		this.port = port;
	}

	@JsonProperty(KEY_ADDRS) private List<String> addrs;
	@JsonProperty(KEY_PORT) private int port;

	public NodeConfig() {
	}

	public NodeConfig(final NodeConfig other) {
		this.addrs = new ArrayList<>(other.getAddrs());
		this.port = other.getPort();
	}

	public List<String> getAddrs() {
		return addrs;
	}

	public int getPort() {
		return port;
	}
}