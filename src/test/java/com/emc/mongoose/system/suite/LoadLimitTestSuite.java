package com.emc.mongoose.system.suite;

import com.emc.mongoose.system.feature.limit.InfiniteWriteTest;
import com.emc.mongoose.system.feature.limit.WriteByCountTest;
import com.emc.mongoose.system.feature.limit.WriteBySizeTest;
import com.emc.mongoose.system.feature.limit.WriteByTimeTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 Created by kurila on 23.05.16.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	InfiniteWriteTest.class,
	WriteByCountTest.class,
	WriteBySizeTest.class,
	WriteByTimeTest.class,
})
public class LoadLimitTestSuite {
	
}
