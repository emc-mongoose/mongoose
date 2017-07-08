package com.emc.mongoose.run.scenario;

import com.emc.mongoose.run.scenario.step.CompositeStep;

/**
 Created by kurila on 02.02.16.
 */
public interface Scenario
extends CompositeStep {
	String DIR_SCENARIO = "scenario";
	String FNAME_DEFAULT_SCENARIO = "default.json";
	String FNAME_SCENARIO_SCHEMA = "schema.json";
}
