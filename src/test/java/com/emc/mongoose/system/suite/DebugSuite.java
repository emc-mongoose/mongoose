package com.emc.mongoose.system.suite;

/**
 Created by kurila on 23.06.16.
 */

import com.emc.mongoose.system.feature.distributed.CircularReadDistributedTest;
import com.emc.mongoose.system.feature.distributed.ReadDirsWithFilesDistributedTest;
import com.emc.mongoose.system.feature.distributed.ReadLoggingDistributedTest;
import com.emc.mongoose.system.feature.reporting.DifferentDestinationLogDirsTest;
import com.emc.mongoose.system.feature.scenario.ForJobTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	CircularReadDistributedTest.class,
	ReadLoggingDistributedTest.class,
	ReadDirsWithFilesDistributedTest.class,
	DifferentDestinationLogDirsTest.class,
	ForJobTest.class
})
public class DebugSuite {
}
