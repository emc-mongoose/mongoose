package com.emc.mongoose.config.scenario;

import com.emc.mongoose.config.scenario.step.StepConfig;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class ScenarioConfig
implements Serializable {

	public static final String KEY_FILE = "file";
	public static final String KEY_STEP = "step";

	public final void setFile(final String file) {
		this.file = file;
	}

	public final void setStepConfig(final StepConfig stepConfig) {
		this.stepConfig = stepConfig;
	}

	@JsonProperty(KEY_FILE) private String file;
	@JsonProperty(KEY_STEP) private StepConfig stepConfig;

	public ScenarioConfig() {
	}

	public ScenarioConfig(final ScenarioConfig other) {
		this.file = other.getFile();
		this.stepConfig = new StepConfig(other.getStepConfig());
	}

	public final String getFile() {
		return file;
	}

	public final StepConfig getStepConfig() {
		return stepConfig;
	}
}
