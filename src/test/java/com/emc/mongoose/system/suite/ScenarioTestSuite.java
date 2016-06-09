package com.emc.mongoose.system.suite;

import com.emc.mongoose.system.feature.scenario.ForEachJobTest;
import com.emc.mongoose.system.feature.scenario.JsonScenarioFileTest;
import com.emc.mongoose.system.feature.scenario.ParallelJobTest;
import com.emc.mongoose.system.feature.scenario.PreconditionJobTest;
import com.emc.mongoose.system.feature.scenario.SequentialJobTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 Created by kurila on 23.05.16.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	JsonScenarioFileTest.class,
	PreconditionJobTest.class,
	SequentialJobTest.class,
	ParallelJobTest.class,
	ForEachJobTest.class,
})
public class ScenarioTestSuite {
}
