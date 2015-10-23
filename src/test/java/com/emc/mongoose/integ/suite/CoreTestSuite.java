package com.emc.mongoose.integ.suite;
//
//
import com.emc.mongoose.integ.core.chain.ConcurrentChainCRUDTest;
import com.emc.mongoose.integ.core.chain.CustomChainScenarioTest;
import com.emc.mongoose.integ.core.chain.DefaultChainScenarioTest;
import com.emc.mongoose.integ.core.chain.SequentialChainCRUDTest;
import com.emc.mongoose.integ.core.rampup.CustomRampupTest;
import com.emc.mongoose.integ.core.rampup.DefaultRampupTest;
import com.emc.mongoose.integ.core.single.CircularRandomReadTest;
import com.emc.mongoose.integ.core.single.CircularReadFromBucketTest;
import com.emc.mongoose.integ.core.single.CircularReadTest;
import com.emc.mongoose.integ.core.single.CircularSingleDataItemReadTest;
import com.emc.mongoose.integ.core.single.DefaultWriteTest;
import com.emc.mongoose.integ.core.single.DeleteManyBucketsConcurrentlyTest;
import com.emc.mongoose.integ.core.single.InfiniteWriteTest;
import com.emc.mongoose.integ.core.single.Read10BItemsTest;
import com.emc.mongoose.integ.core.single.Read10KBItemsTest;
import com.emc.mongoose.integ.core.single.Read10MBItemsTest;
import com.emc.mongoose.integ.core.single.Read200MBItemsTest;
import com.emc.mongoose.integ.core.single.ReadBucketsWithManyObjects;
import com.emc.mongoose.integ.core.single.ReadFewBucketsTest;
import com.emc.mongoose.integ.core.single.ReadRikkiTikkiTaviTest;
import com.emc.mongoose.integ.core.single.ReadVerificationTest;
import com.emc.mongoose.integ.core.single.ReadZeroBytesTest;
import com.emc.mongoose.integ.core.single.ReadZeroSizeItemsTest;
import com.emc.mongoose.integ.core.single.UpdateRikkiTikkiTaviTest;
import com.emc.mongoose.integ.core.single.UpdateZeroBytesTest;
import com.emc.mongoose.integ.core.single.WriteByCountTest;
import com.emc.mongoose.integ.core.single.WriteByTimeTest;
import com.emc.mongoose.integ.core.single.WriteFewBucketsTest;
import com.emc.mongoose.integ.core.single.WriteManyBucketsConcurrentlyTest;
import com.emc.mongoose.integ.core.single.WriteManyObjectsToFewBucketsTest;
import com.emc.mongoose.integ.core.single.WriteRandomSizedItemsTest;
import com.emc.mongoose.integ.core.single.WriteRikkiTikkiTaviTest;
import com.emc.mongoose.integ.core.single.WriteUsing100ConnTest;
import com.emc.mongoose.integ.core.single.WriteUsing10ConnTest;
import com.emc.mongoose.integ.core.single.WriteZeroBytesTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
/**
 * Created by olga on 03.07.15.B
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	DefaultWriteTest.class,
	WriteRandomSizedItemsTest.class,
	ReadZeroSizeItemsTest.class,
	Read10BItemsTest.class,
	Read10KBItemsTest.class,
	Read10MBItemsTest.class,
	Read200MBItemsTest.class,
	WriteUsing10ConnTest.class,
	WriteUsing100ConnTest.class,
	WriteByTimeTest.class,
	ReadVerificationTest.class,
	WriteByCountTest.class,
	CustomChainScenarioTest.class,
	SequentialChainCRUDTest.class,
	ConcurrentChainCRUDTest.class,
	DefaultChainScenarioTest.class,
	CustomRampupTest.class,
	DefaultRampupTest.class,
	InfiniteWriteTest.class,
	CircularReadTest.class,
	CircularSingleDataItemReadTest.class,
	CircularReadFromBucketTest.class,
	CircularRandomReadTest.class,
	WriteZeroBytesTest.class,
	ReadZeroBytesTest.class,
	UpdateZeroBytesTest.class,
	WriteRikkiTikkiTaviTest.class,
	ReadRikkiTikkiTaviTest.class,
	UpdateRikkiTikkiTaviTest.class,
	WriteFewBucketsTest.class,
	WriteManyBucketsConcurrentlyTest.class,
	ReadFewBucketsTest.class,
	DeleteManyBucketsConcurrentlyTest.class,
	WriteManyObjectsToFewBucketsTest.class,
	ReadBucketsWithManyObjects.class,
})
public class CoreTestSuite {}
