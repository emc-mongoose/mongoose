package com.emc.mongoose.system.suite;

/**
 Created by kurila on 23.05.16.
 */

import com.emc.mongoose.system.feature.reporting.DifferentDestinationLogDirsTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	DifferentDestinationLogDirsTest.class,
})
public class ReportingTestSuite {
}
