package com.emc.mongoose.ui.config.load.generator;

import com.emc.mongoose.ui.config.load.generator.recycle.RecycleConfig;
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
	public static final String KEY_RECYCLE = "recycle";
	public static final String KEY_REMOTE = "remote";
	public static final String KEY_SHUFFLE = "shuffle";

	public final void setAddrs(final List<String> addrs) {
		this.addrs = addrs;
	}

	public final void setRecycle(final RecycleConfig recycleConfig) {
		this.recycleConfig = recycleConfig;
	}

	public final void setRemote(final boolean remote) {
		this.remoteFlag = remote;
	}

	public final void setShuffle(final boolean shuffle) {
		this.shuffleFlag = shuffle;
	}

	@JsonProperty(KEY_ADDRS) private List<String> addrs;
	@JsonProperty(KEY_RECYCLE) private RecycleConfig recycleConfig;
	@JsonProperty(KEY_REMOTE) private boolean remoteFlag;
	@JsonProperty(KEY_SHUFFLE) private boolean shuffleFlag;

	public GeneratorConfig() {
	}

	public GeneratorConfig(final GeneratorConfig other) {
		this.addrs = new ArrayList<>(other.getAddrs());
		this.recycleConfig= new RecycleConfig(other.getRecycleConfig());
		this.remoteFlag = other.getRemote();
		this.shuffleFlag = other.getShuffle();
	}

	public final List<String> getAddrs() {
		return addrs;
	}

	public final RecycleConfig getRecycleConfig() {
		return recycleConfig;
	}

	public final boolean getRemote() {
		return remoteFlag;
	}

	public final boolean getShuffle() {
		return shuffleFlag;
	}
}