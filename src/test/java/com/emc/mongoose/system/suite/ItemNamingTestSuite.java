package com.emc.mongoose.system.suite;

import com.emc.mongoose.system.feature.naming.WriteWithAscOrderPrefixedDecimalIdsTest;
import com.emc.mongoose.system.feature.naming.WriteWithDescNamesOrderAndCustomIdRadixTest;
import com.emc.mongoose.system.feature.naming.WriteWithPrefixAndFixedLengthTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 Created by kurila on 23.05.16.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	WriteWithAscOrderPrefixedDecimalIdsTest.class,
	WriteWithDescNamesOrderAndCustomIdRadixTest.class,
	WriteWithPrefixAndFixedLengthTest.class,
})
public class ItemNamingTestSuite {
}
