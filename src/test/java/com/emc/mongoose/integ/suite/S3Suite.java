package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 02.12.15.
 */
import com.emc.mongoose.integ.feature.s3.S3ReadUsingBucketListingDistributedTest;
import com.emc.mongoose.integ.feature.s3.S3ReadUsingBucketListingTest;
import com.emc.mongoose.integ.feature.s3.S3ReadZeroSizedItemsFromBucket;
import com.emc.mongoose.integ.feature.s3.S3UsePreExistingBucketTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
@RunWith(Suite.class)
@Suite.SuiteClasses({
	S3UsePreExistingBucketTest.class,
	S3ReadUsingBucketListingTest.class,
	S3ReadUsingBucketListingDistributedTest.class,
	S3ReadZeroSizedItemsFromBucket.class,
})
public class S3Suite {
}
