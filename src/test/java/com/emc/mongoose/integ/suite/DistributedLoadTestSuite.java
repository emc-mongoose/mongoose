package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 15.07.15.
 */
import com.emc.mongoose.integ.feature.distributed.DeleteLoggingTest;
import com.emc.mongoose.integ.feature.distributed.ReadLoggingTest;
import com.emc.mongoose.integ.feature.distributed.UpdateLoggingTest;
import com.emc.mongoose.integ.feature.distributed.WriteByTimeTest;
import com.emc.mongoose.integ.feature.distributed.WriteLoggingTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
//
@RunWith(Suite.class)
@Suite.SuiteClasses({
	WriteByTimeTest.class,
	WriteLoggingTest.class,
	ReadLoggingTest.class,
	DeleteLoggingTest.class,
	UpdateLoggingTest.class,
})
public class DistributedLoadTestSuite {}
