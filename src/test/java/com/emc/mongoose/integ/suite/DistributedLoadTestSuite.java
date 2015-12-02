package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 15.07.15.
 */
import com.emc.mongoose.integ.feature.distributed.CircularAppendTest;
import com.emc.mongoose.integ.feature.distributed.CircularAppendZeroSizeItems;
import com.emc.mongoose.integ.feature.distributed.CircularReadTest;
import com.emc.mongoose.integ.feature.distributed.CircularSequentialChainTest;
import com.emc.mongoose.integ.feature.distributed.CircularUpdateTest;
import com.emc.mongoose.integ.feature.distributed.DeleteLoggingTest;
import com.emc.mongoose.integ.feature.distributed.RampupTest;
import com.emc.mongoose.integ.feature.distributed.ReadLoggingTest;
import com.emc.mongoose.integ.feature.distributed.SequentialLoadTest;
import com.emc.mongoose.integ.feature.distributed.SimultaneousLoadTest;
import com.emc.mongoose.integ.feature.distributed.UpdateLoggingTest;
import com.emc.mongoose.integ.feature.distributed.UpdateZeroBytesTest;
import com.emc.mongoose.integ.feature.filesystem.WriteToCustomDirTest;
import com.emc.mongoose.integ.feature.distributed.WriteByTimeTest;
import com.emc.mongoose.integ.feature.distributed.WriteLoggingTest;
import com.emc.mongoose.integ.feature.distributed.WriteRikkiTikkiTaviTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
//
@RunWith(Suite.class)
@Suite.SuiteClasses({
	WriteToCustomDirTest.class,
	WriteByTimeTest.class,
	WriteLoggingTest.class,
	ReadLoggingTest.class,
	DeleteLoggingTest.class,
	UpdateLoggingTest.class,
	//
	SequentialLoadTest.class,
	SimultaneousLoadTest.class,
	RampupTest.class,
	//
	CircularReadTest.class,
	CircularAppendTest.class,
	CircularAppendZeroSizeItems.class,
	CircularUpdateTest.class,
	CircularSequentialChainTest.class,
	//
	UpdateZeroBytesTest.class,
	WriteRikkiTikkiTaviTest.class,
	//

})
public class DistributedLoadTestSuite {}
