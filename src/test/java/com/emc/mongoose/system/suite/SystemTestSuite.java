package com.emc.mongoose.system.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 Created by kurila on 17.05.16.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	AtmosTestSuite.class,
	CircularLoadTestSuite.class,
	ContentTestSuite.class,
	CoreTestSuite.class,
	/*DistributedTestSuite.class,*/
	FileSystemTestSuite.class,
	ItemNamingTestSuite.class,
	LoadLimitTestSuite.class,
	LoadTypeTestSuite.class,
	ReportingTestSuite.class,
	S3TestSuite.class,
	ScenarioTestSuite.class,
	SwiftTestSuite.class,
})
public final class SystemTestSuite {
}
