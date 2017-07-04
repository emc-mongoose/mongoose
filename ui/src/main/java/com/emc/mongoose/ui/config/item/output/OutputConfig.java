package com.emc.mongoose.ui.config.item.output;

import com.emc.mongoose.ui.config.TimeStrToLongDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class OutputConfig
implements Serializable {

	public static final String KEY_DELAY = "delay";
	public static final String KEY_FILE = "file";
	public static final String KEY_PATH = "path";

	public final void setDelay(final long delay) {
		this.delay = delay;
	}

	public final void setFile(final String file) {
		this.file = file;
	}

	public final void setPath(final String path) {
		this.path = path;
	}

	@JsonProperty(KEY_DELAY)
	@JsonDeserialize(using=TimeStrToLongDeserializer.class)
	private long delay;

	@JsonProperty(KEY_FILE)
	private String file;

	@JsonProperty(KEY_PATH)
	private String path;

	public OutputConfig() {
	}

	public OutputConfig(final OutputConfig other) {
		this.delay = other.getDelay();
		this.file = other.getFile();
		this.path = other.getPath();
	}

	public long getDelay() {
		return delay;
	}

	public String getFile() {
		return file;
	}

	public String getPath() {
		return path;
	}
}