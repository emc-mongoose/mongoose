package com.emc.mongoose.ui.config.item.data.content;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.item.data.content.ring.RingConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class ContentConfig
implements Serializable {

	public static final String KEY_FILE = "file";
	public static final String KEY_SEED = "seed";
	public static final String KEY_RING = "ring";

	public final void setFile(final String file) {
		this.file = file;
	}

	public final void setSeed(final String seed) {
		this.seed = seed;
	}

	public final void setRingConfig(final RingConfig ringConfig) {
		this.ringConfig = ringConfig;
	}

	@JsonProperty(KEY_FILE) private String file;

	@JsonProperty(KEY_SEED) private String seed;

	@JsonProperty(KEY_RING) private RingConfig ringConfig;

	public ContentConfig() {
	}

	public ContentConfig(final ContentConfig other) {
		this.file = other.getFile();
		this.seed = other.getSeed();
		this.ringConfig = other.getRingConfig();
	}

	public final String getFile() {
		return file;
	}

	public final String getSeed() {
		return seed;
	}

	public final RingConfig getRingConfig() {
		return ringConfig;
	}
}