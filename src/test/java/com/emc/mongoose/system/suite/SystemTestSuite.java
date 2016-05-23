package com.emc.mongoose.system.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 Created by kurila on 17.05.16.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	CircularLoadTestSuite.class,
	CloudStorageApiTestSuite.class,
	ContentTestSuite.class,
	DistributedTestSuite.class,
	DynamicConfigTestSuite.class,
	FileSystemTestSuite.class,
	ItemNamingTestSuite.class,
	LoadLimitTestSuite.class,
	LoadTypeTestSuite.class,
	ReportingTestSuite.class,
	ScenarioTestSuite.class,
})
public final class SystemTestSuite {
}
