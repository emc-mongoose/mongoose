package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 02.12.15.
 */
import com.emc.mongoose.integ.feature.rampup.CustomRampupTest;
import com.emc.mongoose.integ.feature.rampup.DefaultRampupTest;
import com.emc.mongoose.integ.feature.rampup.DistributedRampupTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
@RunWith(Suite.class)
@Suite.SuiteClasses({
	DefaultRampupTest.class,
	CustomRampupTest.class,
	DistributedRampupTest.class,
})
public class RampupScenarioSuite {
}
