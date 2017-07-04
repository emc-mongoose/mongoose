package com.emc.mongoose.ui.config.test.scenario;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class ScenarioConfig
implements Serializable {

	public static final String KEY_FILE = "file";

	public final void setFile(final String file) {
		this.file = file;
	}

	@JsonProperty(KEY_FILE) private String file;

	public ScenarioConfig() {
	}

	public ScenarioConfig(final ScenarioConfig other) {
		this.file = other.getFile();
	}

	public final String getFile() {
		return file;
	}
}