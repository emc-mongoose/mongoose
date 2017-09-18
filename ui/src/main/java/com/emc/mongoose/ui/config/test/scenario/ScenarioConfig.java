package com.emc.mongoose.ui.config.test.scenario;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 Created by andrey on 05.07.17.
 */
public final class ScenarioConfig
implements Serializable {

	public static final String KEY_ENGINES = "engines";
	public static final String KEY_FILE = "file";

	public final void setEngines(final List<String> engines) {
		this.engines = engines;
	}

	public final void setFile(final String file) {
		this.file = file;
	}

	@JsonProperty(KEY_ENGINES) private List<String> engines;
	@JsonProperty(KEY_FILE) private String file;

	public ScenarioConfig() {
	}

	public ScenarioConfig(final ScenarioConfig other) {
		this.engines = new ArrayList<>(other.getEngines());
		this.file = other.getFile();
	}

	public final List<String> getEngines() {
		return engines;
	}

	public final String getFile() {
		return file;
	}
}