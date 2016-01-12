package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 14.08.15.
 */
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
//
@RunWith(Suite.class)
@Suite.SuiteClasses({
	//AtmosSuite.class,
	// CambridgeLabDistributedTestSuite.class, // unstable/experimental
	//ChainScenarioSuite.class,
	CircularitySuite.class,
	//ContainersTestSuite.class,
	//ContentSuite.class,
	CoreTestSuite.class,
	/*DistributedLoadTestSuite.class,
	FileSystemTestSuite.class,
	RampupScenarioSuite.class,
	S3Suite.class,
	SwiftSuite.class,*/
})
public class AllTestSuite {
}
