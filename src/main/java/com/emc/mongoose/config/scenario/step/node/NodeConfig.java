package com.emc.mongoose.config.scenario.step.node;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NodeConfig
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

	public final List<String> getAddrs() {
		return addrs;
	}

	public final int getPort() {
		return port;
	}
}
