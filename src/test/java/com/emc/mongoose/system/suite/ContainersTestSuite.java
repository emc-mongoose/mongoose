package com.emc.mongoose.system.suite;
/**
 Created by andrey on 02.06.16.
 */
import com.emc.mongoose.system.feature.containers.DeleteManyBucketsConcurrentlyTest;
import com.emc.mongoose.system.feature.containers.ReadBucketsWithManyObjectsTest;
import com.emc.mongoose.system.feature.containers.ReadFewBucketsTest;
import com.emc.mongoose.system.feature.containers.WriteFewBucketsTest;
import com.emc.mongoose.system.feature.containers.WriteManyBucketsConcurrentlyTest;
import com.emc.mongoose.system.feature.containers.WriteManyObjectsToFewBucketsTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
@RunWith(Suite.class)
@Suite.SuiteClasses({
	DeleteManyBucketsConcurrentlyTest.class,
	ReadBucketsWithManyObjectsTest.class,
	ReadFewBucketsTest.class,
	WriteFewBucketsTest.class,
	WriteManyBucketsConcurrentlyTest.class,
	WriteManyObjectsToFewBucketsTest.class,
})
public class ContainersTestSuite {
}
