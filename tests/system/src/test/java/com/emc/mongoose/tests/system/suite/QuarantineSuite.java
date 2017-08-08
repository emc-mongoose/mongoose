package com.emc.mongoose.tests.system.suite;

import com.emc.mongoose.tests.system.HttpStorageMetricsThresholdTest;
import com.emc.mongoose.tests.system.TlsAndNodeBalancingTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 Created by kurila on 15.06.17.
 */

@RunWith(Suite.class)

@Suite.SuiteClasses({
	// atmos, 2, 1000, 100MB
	HttpStorageMetricsThresholdTest.class,
	TlsAndNodeBalancingTest.class,
})

public class QuarantineSuite {
}
