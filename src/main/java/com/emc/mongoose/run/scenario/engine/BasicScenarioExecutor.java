package com.emc.mongoose.run.scenario.engine;
//
/**
 Created by kurila on 02.02.16.
 */
public class BasicScenarioExecutor
implements ScenarioExecutor {
	@Override
	public void execute(final Scenario scenario) {
		if(scenario != null) {
			scenario.run();
		}
	}
}
