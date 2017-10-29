package com.emc.mongoose.ui.config.item.data.input;

import com.emc.mongoose.ui.config.item.data.input.layer.LayerConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 24.07.17.
 */
public final class InputConfig
implements Serializable {

	public static final String KEY_FILE = "file";
	public static final String KEY_SEED = "seed";
	public static final String KEY_LAYER = "layer";

	public final void setFile(final String file) {
		this.file = file;
	}

	public final void setSeed(final String seed) {
		this.seed = seed;
	}

	public final void setLayerConfig(final LayerConfig layerConfig) {
		this.layerConfig = layerConfig;
	}

	@JsonProperty(KEY_FILE) private String file;

	@JsonProperty(KEY_SEED) private String seed;

	@JsonProperty(KEY_LAYER) private LayerConfig layerConfig;

	public InputConfig() {
	}

	public InputConfig(final InputConfig other) {
		this.file = other.getFile();
		this.seed = other.getSeed();
		this.layerConfig = new LayerConfig(other.getLayerConfig());
	}

	public final String getFile() {
		return file;
	}

	public final String getSeed() {
		return seed;
	}

	public final LayerConfig getLayerConfig() {
		return layerConfig;
	}
}
