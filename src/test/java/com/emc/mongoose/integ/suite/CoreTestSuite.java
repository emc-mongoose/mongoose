package com.emc.mongoose.integ.suite;
//
//
import com.emc.mongoose.integ.core.single.ReadContainersWithManyObjects;
import com.emc.mongoose.integ.core.single.WriteManyObjectsToFewContainersTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
/**
 * Created by olga on 03.07.15.B
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	/*DefaultWriteTest.class,
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
	WriteFewContainersTest.class,
	WriteManyContainersConcurrentlyTest.class,
	ReadFewContainersTest.class,
	DeleteManyContainersConcurrentlyTest.class,
	WriteManyObjectsToFewContainersTest.class,*/
	ReadContainersWithManyObjects.class,
})
public class CoreTestSuite {}
