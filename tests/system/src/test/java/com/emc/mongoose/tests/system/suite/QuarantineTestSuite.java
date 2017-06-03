package com.emc.mongoose.tests.system.suite;

import com.emc.mongoose.tests.system.CreateByCountTest;
import com.emc.mongoose.tests.system.ReadMultipleRandomFileRangesTest;
import com.emc.mongoose.tests.system.ReadSmallDataItemsMetricsThresholdTest;
import com.emc.mongoose.tests.system.S3MpuTest;

import org.junit.runners.Suite;
import org.junit.runner.RunWith;

/**
 Created by andrey on 03.06.17.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	CreateByCountTest.class,
	ReadMultipleRandomFileRangesTest.class,
	ReadSmallDataItemsMetricsThresholdTest.class,
	S3MpuTest.class,
})
public final class QuarantineTestSuite {
}
