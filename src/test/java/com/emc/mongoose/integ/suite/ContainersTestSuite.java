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
})
public class ContainersTestSuite {
}
