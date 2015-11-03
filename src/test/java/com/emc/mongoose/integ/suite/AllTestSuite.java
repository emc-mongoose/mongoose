package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 14.08.15.
 */
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
//
@RunWith(Suite.class)
@Suite.SuiteClasses({
	//CambridgeLabDistributedTestSuite.class,
	CoreTestSuite.class,
	DistributedLoadTestSuite.class,
	StorageAdapterTestSuite.class,
})
public class AllTestSuite {
}
