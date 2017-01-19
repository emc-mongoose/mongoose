package com.emc.mongoose.tests.system.suite;

import com.emc.mongoose.tests.system.feature.limit.DistributedCreateByTimeTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 Created by andrey on 19.01.17.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	DistributedCreateByTimeTest.class
})
public class DistributedTestSuite {
}
