package com.emc.mongoose.integ.suite;

//
import com.emc.mongoose.integ.core.api.atmos.AtmosMultiRangeUpdateTest;
import com.emc.mongoose.integ.core.api.atmos.AtmosReadUsingCSVInputTest;
import com.emc.mongoose.integ.core.api.atmos.AtmosSingleRangeUpdateTest;
import com.emc.mongoose.integ.core.api.atmos.AtmosUsePreExistingSubtenantTest;
import com.emc.mongoose.integ.core.api.atmos.AtmosWriteByCountTest;
import com.emc.mongoose.integ.core.api.s3.S3ReadUsingBucketListingTest;
import com.emc.mongoose.integ.core.api.s3.S3UsePreExistingBucketTest;
//
import com.emc.mongoose.integ.core.api.swift.SwiftReadUsingContainerListingTest;
import com.emc.mongoose.integ.core.api.swift.SwiftUsePreExistingAuthTokenTest;
import com.emc.mongoose.integ.core.api.swift.SwiftUsePreExistingContainerTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
//
/**
 * Created by olga on 03.07.15.
 */
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
	/*DefaultWriteTest.class,
	WriteRandomSizedItemsTest.class,
	ReadZeroSizeItemsTest.class,
	Read10BItemsTest.class,
	Read10KBItemsTest.class,
	Read10MBItemsTest.class,t
	Read200MBItemsTest.class,
	WriteUsing10ConnTest.class,
	WriteUsing100ConnTest.class,
	WriteByTimeTest.class,
	ReadVerificationTest.class,
	WriteByCountTest.class,
	CustomChainScenarioIntegTest.class,
	CRUDSequentialScenarioIntegTest.class,
	CRUDSimultaneousScenarioIntegTest.class,
	DefaultChainScenarioIntegTest.class,
	CustomRampupTest.class,
	DefaultRampupTest.class,
	InfiniteWriteTest.class*/
})
public class CoreTestSuite
extends WSMockTestSuite {

	@BeforeClass
	public static void startCinderella()
	throws Exception {
	}

	@AfterClass
	public static void interruptCinderella()
	throws Exception {
	}
}
