package com.emc.mongoose.ui.config.test;

import com.emc.mongoose.ui.config.test.scenario.ScenarioConfig;
import com.emc.mongoose.ui.config.test.step.StepConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class TestConfig
implements Serializable {

	public static final String KEY_SCENARIO = "scenario";
	public static final String KEY_STEP = "step";

	@JsonProperty(KEY_SCENARIO)
	private ScenarioConfig scenarioConfig;
	@JsonProperty(KEY_STEP)
	private StepConfig stepConfig;

	public final ScenarioConfig getScenarioConfig() {
		return this.scenarioConfig;
	}

	public final StepConfig getStepConfig() {
		return this.stepConfig;
	}

	public final void setScenarioConfig(final ScenarioConfig scenarioConfig) {
		this.scenarioConfig = scenarioConfig;
	}

	public final void setStepConfig(final StepConfig stepConfig) {
		this.stepConfig = stepConfig;
	}

	public TestConfig() {
	}

	public TestConfig(final TestConfig other) {
		this.scenarioConfig = new ScenarioConfig(other.getScenarioConfig());
		this.stepConfig = new StepConfig(other.getStepConfig());
	}
}