package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 15.02.16.
 */
import com.emc.mongoose.integ.feature.naming.WriteWithAscOrderPrefixedDecimalIdsTest;
import com.emc.mongoose.integ.feature.naming.WriteWithDescNamesOrderAndCustomIdRadixTest;
import com.emc.mongoose.integ.feature.naming.WriteWithPrefixAndFixedLengthTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
@RunWith(Suite.class)
@Suite.SuiteClasses({
	WriteWithAscOrderPrefixedDecimalIdsTest.class,
	WriteWithDescNamesOrderAndCustomIdRadixTest.class,
	WriteWithPrefixAndFixedLengthTest.class,
})
public class NamingSuite {
}
