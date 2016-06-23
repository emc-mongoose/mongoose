package com.emc.mongoose.system.suite;

/**
 Created by kurila on 23.05.16.
 */

import com.emc.mongoose.system.feature.distributed.DeleteManyBucketsConcurrentlyDistributedTest;
import com.emc.mongoose.system.feature.distributed.ReadBucketsWithManyObjectsDistributedTest;
import com.emc.mongoose.system.feature.distributed.ReadFewBucketsDistributedTest;
import com.emc.mongoose.system.feature.distributed.UpdateZeroBytesDistributedTest;
import com.emc.mongoose.system.feature.distributed.WriteFewBucketsDistributedTest;
import com.emc.mongoose.system.feature.distributed.WriteManyBucketsConcurrentlyDistributedTest;
import com.emc.mongoose.system.feature.distributed.WriteManyObjectsToFewBucketsDistributedTest;
import com.emc.mongoose.system.feature.distributed.CircularReadDistributedTest;
import com.emc.mongoose.system.feature.distributed.CircularUpdateTest;
import com.emc.mongoose.system.feature.distributed.DeleteLoggingTest;
import com.emc.mongoose.system.feature.distributed.ReadDirsWithFilesDistributedTest;
import com.emc.mongoose.system.feature.distributed.ReadLoggingDistributedTest;
import com.emc.mongoose.system.feature.distributed.S3ReadUsingBucketListingDistributedTest;
import com.emc.mongoose.system.feature.distributed.UpdateLoggingTest;
import com.emc.mongoose.system.feature.distributed.WriteByTimeTest;
import com.emc.mongoose.system.feature.distributed.WriteLoggingTest;
import com.emc.mongoose.system.feature.distributed.WriteRikkiTikkiTaviDistributedTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	CircularReadDistributedTest.class,
	CircularUpdateTest.class,
	WriteLoggingTest.class,
	WriteByTimeTest.class,
	ReadDirsWithFilesDistributedTest.class,
	ReadLoggingDistributedTest.class,
	S3ReadUsingBucketListingDistributedTest.class,
	UpdateLoggingTest.class,
	DeleteLoggingTest.class,
	DeleteManyBucketsConcurrentlyDistributedTest.class,
	ReadBucketsWithManyObjectsDistributedTest.class,
	ReadFewBucketsDistributedTest.class,
	WriteFewBucketsDistributedTest.class,
	WriteManyBucketsConcurrentlyDistributedTest.class,
	WriteManyObjectsToFewBucketsDistributedTest.class,
	UpdateZeroBytesDistributedTest.class,
	WriteRikkiTikkiTaviDistributedTest.class,
})
public class DistributedTestSuite {
}
