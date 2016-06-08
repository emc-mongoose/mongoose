package com.emc.mongoose.system.suite;

import com.emc.mongoose.system.feature.scenario.ForEachJobTest;
import com.emc.mongoose.system.feature.scenario.JsonScenarioFileTest;
import com.emc.mongoose.system.feature.scenario.PreconditionJobTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 Created by kurila on 23.05.16.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	JsonScenarioFileTest.class,
	PreconditionJobTest.class,
	ForEachJobTest.class,
})
public class ScenarioTestSuite {
}
