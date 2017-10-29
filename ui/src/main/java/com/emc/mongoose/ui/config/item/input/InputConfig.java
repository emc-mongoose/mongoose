package com.emc.mongoose.ui.config.item.input;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class InputConfig
implements Serializable {

	public static final String KEY_PATH = "path";
	public static final String KEY_FILE = "file";

	public final void setPath(final String path) {
		this.path = path;
	}

	public final void setFile(final String file) {
		this.file = file;
	}

	@JsonProperty(KEY_PATH) private String path;
	@JsonProperty(KEY_FILE) private String file;

	public InputConfig() {
	}

	public InputConfig(final InputConfig other) {
		this.path = other.getPath();
		this.file = other.getFile();
	}

	public final String getPath() {
		return path;
	}

	public final String getFile() {
		return file;
	}

}