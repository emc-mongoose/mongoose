package com.emc.mongoose.system.suite;
import com.emc.mongoose.system.feature.s3.S3ReadUsingBucketListingTest;
import com.emc.mongoose.system.feature.s3.S3ReadZeroSizedItemsFromBucket;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
/**
 Created by andrey on 30.05.16.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	S3ReadUsingBucketListingTest.class,
	S3ReadZeroSizedItemsFromBucket.class,
})
public class S3TestSuite {
}
