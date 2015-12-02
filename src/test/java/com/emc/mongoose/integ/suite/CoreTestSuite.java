package com.emc.mongoose.integ.suite;
//
//
import com.emc.mongoose.integ.feature.scenario.CircularSequentialChainTest;
import com.emc.mongoose.integ.feature.scenario.ConcurrentChainCRUDTest;
import com.emc.mongoose.integ.feature.scenario.CustomChainScenarioTest;
import com.emc.mongoose.integ.feature.scenario.DefaultChainScenarioTest;
import com.emc.mongoose.integ.feature.scenario.SequentialChainCRUDTest;
import com.emc.mongoose.integ.feature.scenario.CustomRampupTest;
import com.emc.mongoose.integ.feature.scenario.DefaultRampupTest;
import com.emc.mongoose.integ.feature.circularity.CircularAppendTest;
import com.emc.mongoose.integ.feature.circularity.CircularReadAfterUpdateTest;
import com.emc.mongoose.integ.feature.circularity.CircularReadFromBucketTest;
import com.emc.mongoose.integ.feature.circularity.CircularReadTest;
import com.emc.mongoose.integ.feature.circularity.CircularUpdateTest;
import com.emc.mongoose.integ.feature.core.DefaultWriteTest;
import com.emc.mongoose.integ.feature.containers.DeleteManyBucketsConcurrentlyTest;
import com.emc.mongoose.integ.feature.core.InfiniteWriteTest;
import com.emc.mongoose.integ.feature.core.Read10BItemsTest;
import com.emc.mongoose.integ.feature.core.Read10KBItemsTest;
import com.emc.mongoose.integ.feature.core.Read10MBItemsTest;
import com.emc.mongoose.integ.feature.core.Read200MBItemsTest;
import com.emc.mongoose.integ.feature.containers.ReadBucketsWithManyObjects;
import com.emc.mongoose.integ.feature.containers.ReadFewBucketsTest;
import com.emc.mongoose.integ.feature.content.ReadRikkiTikkiTaviTest;
import com.emc.mongoose.integ.feature.core.ReadVerificationTest;
import com.emc.mongoose.integ.feature.content.ReadZeroBytesTest;
import com.emc.mongoose.integ.feature.core.ReadZeroSizeItemsTest;
import com.emc.mongoose.integ.feature.content.UpdateRikkiTikkiTaviTest;
import com.emc.mongoose.integ.feature.content.UpdateZeroBytesTest;
import com.emc.mongoose.integ.feature.core.WriteByCountTest;
import com.emc.mongoose.integ.feature.core.WriteByTimeTest;
import com.emc.mongoose.integ.feature.containers.WriteFewBucketsTest;
import com.emc.mongoose.integ.feature.containers.WriteManyBucketsConcurrentlyTest;
import com.emc.mongoose.integ.feature.containers.WriteManyObjectsToFewBucketsTest;
import com.emc.mongoose.integ.feature.core.WriteRandomSizedItemsTest;
import com.emc.mongoose.integ.feature.content.WriteRikkiTikkiTaviTest;
import com.emc.mongoose.integ.feature.core.WriteUsing100ConnTest;
import com.emc.mongoose.integ.feature.core.WriteUsing10ConnTest;
import com.emc.mongoose.integ.feature.content.WriteZeroBytesTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
/**
 * Created by olga on 03.07.15.B
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	DefaultWriteTest.class,
	InfiniteWriteTest.class,
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
	//
	CustomChainScenarioTest.class,
	SequentialChainCRUDTest.class,
	ConcurrentChainCRUDTest.class,
	DefaultChainScenarioTest.class,
	CustomRampupTest.class,
	DefaultRampupTest.class,
	//
	CircularAppendTest.class,
	CircularReadAfterUpdateTest.class,
	CircularReadTest.class,
	CircularReadFromBucketTest.class,
	CircularUpdateTest.class,
	CircularSequentialChainTest.class,
	//
	WriteZeroBytesTest.class,
	ReadZeroBytesTest.class,
	UpdateZeroBytesTest.class,
	WriteRikkiTikkiTaviTest.class,
	ReadRikkiTikkiTaviTest.class,
	UpdateRikkiTikkiTaviTest.class,
})
public class CoreTestSuite {}
