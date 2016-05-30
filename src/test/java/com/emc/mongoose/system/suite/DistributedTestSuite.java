package com.emc.mongoose.system.suite;

/**
 Created by kurila on 23.05.16.
 */

import com.emc.mongoose.system.feature.distributed.DeleteLoggingTest;
import com.emc.mongoose.system.feature.distributed.ReadLoggingTest;
import com.emc.mongoose.system.feature.distributed.UpdateLoggingTest;
import com.emc.mongoose.system.feature.distributed.WriteByTimeTest;
import com.emc.mongoose.system.feature.distributed.WriteLoggingTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	WriteLoggingTest.class,
	WriteByTimeTest.class,
	ReadLoggingTest.class,
	UpdateLoggingTest.class,
	DeleteLoggingTest.class,
})
public class DistributedTestSuite {
}
