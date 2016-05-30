package com.emc.mongoose.system.suite;

/**
 Created by kurila on 23.05.16.
 */

import com.emc.mongoose.system.feature.distributed.CircularReadTest;
import com.emc.mongoose.system.feature.distributed.CircularUpdateTest;
import com.emc.mongoose.system.feature.distributed.DeleteLoggingTest;
import com.emc.mongoose.system.feature.distributed.ReadDirsWithFilesTest;
import com.emc.mongoose.system.feature.distributed.ReadLoggingTest;
import com.emc.mongoose.system.feature.distributed.S3ReadUsingBucketListingDistributedTest;
import com.emc.mongoose.system.feature.distributed.UpdateLoggingTest;
import com.emc.mongoose.system.feature.distributed.WriteByTimeTest;
import com.emc.mongoose.system.feature.distributed.WriteLoggingTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	CircularReadTest.class,
	CircularUpdateTest.class,
	WriteLoggingTest.class,
	WriteByTimeTest.class,
	ReadDirsWithFilesTest.class,
	ReadLoggingTest.class,
	S3ReadUsingBucketListingDistributedTest.class,
	UpdateLoggingTest.class,
	DeleteLoggingTest.class,
})
public class DistributedTestSuite {
}
