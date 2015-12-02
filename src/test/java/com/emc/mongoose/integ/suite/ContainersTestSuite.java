package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 02.12.15.
 */
import com.emc.mongoose.integ.feature.containers.DeleteManyBucketsConcurrentlyTest;
import com.emc.mongoose.integ.feature.containers.ReadBucketsWithManyObjects;
import com.emc.mongoose.integ.feature.containers.ReadFewBucketsTest;
import com.emc.mongoose.integ.feature.containers.WriteFewBucketsTest;
import com.emc.mongoose.integ.feature.containers.WriteManyBucketsConcurrentlyTest;
import com.emc.mongoose.integ.feature.containers.WriteManyObjectsToFewBucketsTest;
import com.emc.mongoose.integ.feature.containers.DeleteManyBucketsConcurrentlyDistributedTest;
import com.emc.mongoose.integ.feature.containers.ReadBucketsWithManyObjectsDistributedTest;
import com.emc.mongoose.integ.feature.containers.ReadFewBucketsDistributedTest;
import com.emc.mongoose.integ.feature.containers.WriteFewBucketsDistributedTest;
import com.emc.mongoose.integ.feature.containers.WriteManyBucketsConcurrentlyDistributedTest;
import com.emc.mongoose.integ.feature.containers.WriteManyObjectsToFewBucketsDistributedTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
@RunWith(Suite.class)
@Suite.SuiteClasses({
	WriteFewBucketsTest.class,
	WriteManyBucketsConcurrentlyTest.class,
	ReadFewBucketsTest.class,
	DeleteManyBucketsConcurrentlyTest.class,
	WriteManyObjectsToFewBucketsTest.class,
	ReadBucketsWithManyObjects.class,
	//
	WriteFewBucketsDistributedTest.class,
	WriteManyBucketsConcurrentlyDistributedTest.class,
	ReadFewBucketsDistributedTest.class,
	DeleteManyBucketsConcurrentlyDistributedTest.class,
	WriteManyObjectsToFewBucketsDistributedTest.class,
	ReadBucketsWithManyObjectsDistributedTest.class,
})
public class ContainersTestSuite {
}
