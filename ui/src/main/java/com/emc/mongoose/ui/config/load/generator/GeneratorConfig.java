package com.emc.mongoose.ui.config.load.generator;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 Created by andrey on 05.07.17.
 */
public final class GeneratorConfig
implements Serializable {

	public static final String KEY_ADDRS = "addrs";
	public static final String KEY_REMOTE = "remote";
	public static final String KEY_SHUFFLE = "shuffle";

	public final void setAddrs(final List<String> addrs) {
		this.addrs = addrs;
	}

	public final void setRemote(final boolean remote) {
		this.remote = remote;
	}

	public final void setShuffle(final boolean shuffle) {
		this.shuffle = shuffle;
	}

	@JsonProperty(KEY_ADDRS) private List<String> addrs;
	@JsonProperty(KEY_REMOTE) private boolean remote;
	@JsonProperty(KEY_SHUFFLE) private boolean shuffle;

	public GeneratorConfig() {
	}

	public GeneratorConfig(final GeneratorConfig other) {
		this.addrs = new ArrayList<>(other.getAddrs());
		this.remote = other.getRemote();
		this.shuffle = other.getShuffle();
	}

	public List<String> getAddrs() {
		return addrs;
	}

	public boolean getRemote() {
		return remote;
	}

	public boolean getShuffle() {
		return shuffle;
	}
}