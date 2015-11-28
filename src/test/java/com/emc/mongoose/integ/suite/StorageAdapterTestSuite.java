package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 14.08.15.
 */
import com.emc.mongoose.integ.feature.storage.web.atmos.AtmosMultiRangeUpdateTest;
import com.emc.mongoose.integ.feature.storage.web.atmos.AtmosReadUsingCSVInputTest;
import com.emc.mongoose.integ.feature.storage.web.atmos.AtmosSingleRangeUpdateTest;
import com.emc.mongoose.integ.feature.storage.web.atmos.AtmosUsePreExistingSubtenantTest;
import com.emc.mongoose.integ.feature.storage.web.atmos.AtmosWriteByCountTest;
import com.emc.mongoose.integ.feature.storage.web.s3.S3ReadUsingBucketListingTest;
import com.emc.mongoose.integ.feature.storage.web.s3.S3UsePreExistingBucketTest;
import com.emc.mongoose.integ.feature.storage.web.swift.DeleteManyContainersConcurrentlyTest;
import com.emc.mongoose.integ.feature.storage.web.swift.ReadContainersWithManyObjects;
import com.emc.mongoose.integ.feature.storage.web.swift.ReadFewContainersTest;
import com.emc.mongoose.integ.feature.storage.web.swift.SwiftReadUsingContainerListingTest;
import com.emc.mongoose.integ.feature.storage.web.swift.SwiftUsePreExistingAuthTokenTest;
import com.emc.mongoose.integ.feature.storage.web.swift.SwiftUsePreExistingContainerTest;
//
import com.emc.mongoose.integ.feature.storage.web.swift.WriteFewContainersTest;
import com.emc.mongoose.integ.feature.storage.web.swift.WriteManyContainersConcurrentlyTest;
import com.emc.mongoose.integ.feature.storage.web.swift.WriteManyObjectsToFewContainersTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
//
@RunWith(Suite.class)
@Suite.SuiteClasses({
	AtmosMultiRangeUpdateTest.class,
	AtmosReadUsingCSVInputTest.class,
	AtmosSingleRangeUpdateTest.class,
	AtmosUsePreExistingSubtenantTest.class,
	AtmosWriteByCountTest.class,
	S3UsePreExistingBucketTest.class,
	S3ReadUsingBucketListingTest.class,
	SwiftReadUsingContainerListingTest.class,
	SwiftUsePreExistingAuthTokenTest.class,
	SwiftUsePreExistingContainerTest.class,
	DeleteManyContainersConcurrentlyTest.class,
	ReadContainersWithManyObjects.class,
	ReadFewContainersTest.class,
	WriteFewContainersTest.class,
	WriteManyContainersConcurrentlyTest.class,
	WriteManyObjectsToFewContainersTest.class,
})
public class StorageAdapterTestSuite {
}
