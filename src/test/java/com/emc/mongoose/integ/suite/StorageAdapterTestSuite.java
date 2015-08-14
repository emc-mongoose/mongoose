package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 14.08.15.
 */
import com.emc.mongoose.integ.storage.adapter.atmos.AtmosMultiRangeUpdateTest;
import com.emc.mongoose.integ.storage.adapter.atmos.AtmosReadUsingCSVInputTest;
import com.emc.mongoose.integ.storage.adapter.atmos.AtmosSingleRangeUpdateTest;
import com.emc.mongoose.integ.storage.adapter.atmos.AtmosUsePreExistingSubtenantTest;
import com.emc.mongoose.integ.storage.adapter.atmos.AtmosWriteByCountTest;
import com.emc.mongoose.integ.storage.adapter.s3.S3ReadUsingBucketListingTest;
import com.emc.mongoose.integ.storage.adapter.s3.S3UsePreExistingBucketTest;
import com.emc.mongoose.integ.storage.adapter.swift.SwiftReadUsingContainerListingTest;
import com.emc.mongoose.integ.storage.adapter.swift.SwiftUsePreExistingAuthTokenTest;
import com.emc.mongoose.integ.storage.adapter.swift.SwiftUsePreExistingContainerTest;
//
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
})
public class StorageAdapterTestSuite {
}
