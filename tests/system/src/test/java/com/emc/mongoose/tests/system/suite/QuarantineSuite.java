package com.emc.mongoose.tests.system.suite;

import com.emc.mongoose.tests.system.MultipleFixedUpdateAndSingleFixedReadTest;
import com.emc.mongoose.tests.system.SingleFixedUpdateAndSingleRandomReadTest;
import com.emc.mongoose.tests.system.TlsAndNodeBalancingTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 Created by kurila on 15.06.17.
 */

@RunWith(Suite.class)

@Suite.SuiteClasses({
	MultipleFixedUpdateAndSingleFixedReadTest.class,
	SingleFixedUpdateAndSingleRandomReadTest.class,
	TlsAndNodeBalancingTest.class,
})

public class QuarantineSuite {
}
