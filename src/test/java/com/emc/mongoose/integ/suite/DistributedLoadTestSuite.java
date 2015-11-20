package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 15.07.15.
 */
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
//
@RunWith(Suite.class)
@Suite.SuiteClasses({
	com.emc.mongoose.integ.distributed.single.WriteByCountTest.class,
	com.emc.mongoose.integ.distributed.single.WriteByTimeTest.class,
	com.emc.mongoose.integ.distributed.single.WriteLoggingTest.class,
	com.emc.mongoose.integ.distributed.single.ReadLoggingTest.class,
	com.emc.mongoose.integ.distributed.single.DeleteLoggingTest.class,
	com.emc.mongoose.integ.distributed.single.UpdateLoggingTest.class,
	com.emc.mongoose.integ.distributed.chain.SequentialLoadTest.class,
	com.emc.mongoose.integ.distributed.chain.SimultaneousLoadTest.class,
	com.emc.mongoose.integ.distributed.rampup.RampupTest.class,
	com.emc.mongoose.integ.distributed.single.CircularReadTest.class,
	com.emc.mongoose.integ.distributed.single.CircularAppendTest.class,
	com.emc.mongoose.integ.distributed.single.CircularAppendZeroSizeItems.class,
	com.emc.mongoose.integ.distributed.single.CircularUpdateTest.class,
	com.emc.mongoose.integ.distributed.chain.CircularSequentialChainTest.class,
	com.emc.mongoose.integ.distributed.single.UpdateZeroBytesTest.class,
	com.emc.mongoose.integ.distributed.single.WriteRikkiTikkiTaviTest.class,
	com.emc.mongoose.integ.distributed.single.WriteFewBucketsTest.class,
	com.emc.mongoose.integ.distributed.single.WriteManyBucketsConcurrentlyTest.class,
	com.emc.mongoose.integ.distributed.single.ReadFewBucketsTest.class,
	com.emc.mongoose.integ.distributed.single.DeleteManyBucketsConcurrentlyTest.class,
	com.emc.mongoose.integ.distributed.single.WriteManyObjectsToFewBucketsTest.class,
	com.emc.mongoose.integ.distributed.single.ReadBucketsWithManyObjects.class,
})
public class DistributedLoadTestSuite {}
