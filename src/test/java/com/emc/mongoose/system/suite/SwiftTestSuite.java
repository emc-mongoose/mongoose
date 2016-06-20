package com.emc.mongoose.system.suite;
import com.emc.mongoose.system.feature.swift.SwiftDeleteManyContainersConcurrentlyTest;
import com.emc.mongoose.system.feature.swift.SwiftReadContainersWithManyObjects;
import com.emc.mongoose.system.feature.swift.SwiftReadFewContainersTest;
import com.emc.mongoose.system.feature.swift.SwiftReadRandomSizedItemsFromContainer;
import com.emc.mongoose.system.feature.swift.SwiftReadUsingContainerListingTest;
import com.emc.mongoose.system.feature.swift.SwiftWriteFewContainersTest;
import com.emc.mongoose.system.feature.swift.SwiftWriteManyContainersConcurrentlyTest;
import com.emc.mongoose.system.feature.swift.SwiftWriteManyObjectsToFewContainersTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
/**
 Created by andrey on 30.05.16.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	SwiftDeleteManyContainersConcurrentlyTest.class,
	SwiftReadContainersWithManyObjects.class,
	SwiftReadFewContainersTest.class,
	SwiftReadRandomSizedItemsFromContainer.class,
	SwiftReadUsingContainerListingTest.class,
	SwiftWriteFewContainersTest.class,
	SwiftWriteManyContainersConcurrentlyTest.class,
	SwiftWriteManyObjectsToFewContainersTest.class,
})
public class SwiftTestSuite {
}
