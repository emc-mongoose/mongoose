package com.emc.mongoose.config.load.generator;

import com.emc.mongoose.config.load.generator.recycle.RecycleConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class GeneratorConfig
implements Serializable {

	public static final String KEY_RECYCLE = "recycle";
	public static final String KEY_REMOTE = "remote";
	public static final String KEY_SHUFFLE = "shuffle";
	public static final String KEY_WEIGHT = "weight";

	public final void setRecycle(final RecycleConfig recycleConfig) {
		this.recycleConfig = recycleConfig;
	}

	public final void setRemote(final boolean remote) {
		this.remoteFlag = remote;
	}

	public final void setShuffle(final boolean shuffle) {
		this.shuffleFlag = shuffle;
	}

	public final void setWeight(final int weight) {
		this.weight = weight;
	}

	@JsonProperty(KEY_RECYCLE) private RecycleConfig recycleConfig;
	@JsonProperty(KEY_REMOTE) private boolean remoteFlag;
	@JsonProperty(KEY_SHUFFLE) private boolean shuffleFlag;
	@JsonProperty(KEY_WEIGHT) private int weight;

	public GeneratorConfig() {
	}

	public GeneratorConfig(final GeneratorConfig other) {
		this.recycleConfig= new RecycleConfig(other.getRecycleConfig());
		this.remoteFlag = other.getRemote();
		this.shuffleFlag = other.getShuffle();
		this.weight = other.getWeight();
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

	public final int getWeight() {
		return weight;
	}
}
