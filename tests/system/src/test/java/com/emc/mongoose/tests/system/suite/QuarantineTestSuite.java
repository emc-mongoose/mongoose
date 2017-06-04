package com.emc.mongoose.tests.system.suite;

import com.emc.mongoose.tests.system.deprecated.CreateByCountTest;
import com.emc.mongoose.tests.system.deprecated.ReadBucketListingTest;
import com.emc.mongoose.tests.system.deprecated.ReadMultipleRandomFileRangesTest;
import com.emc.mongoose.tests.system.deprecated.ReadSmallDataItemsMetricsThresholdTest;
import com.emc.mongoose.tests.system.deprecated.S3MpuTest;

import org.junit.Ignore;
import org.junit.runners.Suite;
import org.junit.runner.RunWith;

/**
 Created by andrey on 03.06.17.
 */
@Ignore
@RunWith(Suite.class)
@Suite.SuiteClasses({
	CreateByCountTest.class,
	ReadBucketListingTest.class,
	ReadMultipleRandomFileRangesTest.class,
	ReadSmallDataItemsMetricsThresholdTest.class,
	S3MpuTest.class,
})
public final class QuarantineTestSuite {
}
