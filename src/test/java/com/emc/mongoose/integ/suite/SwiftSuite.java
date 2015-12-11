package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 14.08.15.
 */
import com.emc.mongoose.integ.feature.swift.DeleteManyContainersConcurrentlyTest;
import com.emc.mongoose.integ.feature.swift.ReadContainersWithManyObjects;
import com.emc.mongoose.integ.feature.swift.ReadFewContainersTest;
import com.emc.mongoose.integ.feature.swift.SwiftReadUsingContainerListingTest;
import com.emc.mongoose.integ.feature.swift.SwiftUsePreExistingAuthTokenTest;
import com.emc.mongoose.integ.feature.swift.SwiftUsePreExistingContainerTest;
import com.emc.mongoose.integ.feature.swift.WriteFewContainersTest;
import com.emc.mongoose.integ.feature.swift.WriteManyContainersConcurrentlyTest;
import com.emc.mongoose.integ.feature.swift.WriteManyObjectsToFewContainersTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
//
@RunWith(Suite.class)
@Suite.SuiteClasses({
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
public class SwiftSuite {
}
