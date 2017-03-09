package com.emc.mongoose.run.scenario;

import com.emc.mongoose.run.scenario.job.Job;

/**
 Created by kurila on 02.02.16.
 */
public interface Scenario
extends Job {
	String DIR_SCENARIO = "scenario";
	String FNAME_DEFAULT_SCENARIO = "default.json";
	String FNAME_SCENARIO_SCHEMA = "schema.json";
}
