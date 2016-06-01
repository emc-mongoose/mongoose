package com.emc.mongoose.system.suite;
/**
 Created by andrey on 30.05.16.
 */
import com.emc.mongoose.system.feature.atmos.AtmosMultiRangeUpdateTest;
import com.emc.mongoose.system.feature.atmos.AtmosReadUsingCsvInputTest;
import com.emc.mongoose.system.feature.atmos.AtmosSingleRangeUpdateTest;
import com.emc.mongoose.system.feature.atmos.AtmosWriteByCountTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
@RunWith(Suite.class)
@Suite.SuiteClasses({
	AtmosMultiRangeUpdateTest.class,
	AtmosReadUsingCsvInputTest.class,
	AtmosSingleRangeUpdateTest.class,
	AtmosWriteByCountTest.class,
})
public class AtmosTestSuite {
}
