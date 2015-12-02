package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 02.12.15.
 */
import com.emc.mongoose.integ.feature.atmos.AtmosMultiRangeUpdateTest;
import com.emc.mongoose.integ.feature.atmos.AtmosReadUsingCSVInputTest;
import com.emc.mongoose.integ.feature.atmos.AtmosSingleRangeUpdateTest;
import com.emc.mongoose.integ.feature.atmos.AtmosUsePreExistingSubtenantTest;
import com.emc.mongoose.integ.feature.atmos.AtmosWriteByCountTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
@RunWith(Suite.class)
@Suite.SuiteClasses({
	AtmosMultiRangeUpdateTest.class,
	AtmosReadUsingCSVInputTest.class,
	AtmosSingleRangeUpdateTest.class,
	AtmosUsePreExistingSubtenantTest.class,
	AtmosWriteByCountTest.class,
})
public class AtmosSuite {
}
