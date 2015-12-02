package com.emc.mongoose.integ.suite;
//
//
import com.emc.mongoose.integ.feature.chain.ConcurrentChainCRUDTest;
import com.emc.mongoose.integ.feature.chain.CustomChainScenarioTest;
import com.emc.mongoose.integ.feature.chain.DefaultChainScenarioTest;
import com.emc.mongoose.integ.feature.chain.SequentialChainCRUDTest;
import com.emc.mongoose.integ.feature.rampup.CustomRampupTest;
import com.emc.mongoose.integ.feature.rampup.DefaultRampupTest;
import com.emc.mongoose.integ.feature.core.DefaultWriteTest;
import com.emc.mongoose.integ.feature.core.InfiniteWriteTest;
import com.emc.mongoose.integ.feature.core.Read10BItemsTest;
import com.emc.mongoose.integ.feature.core.Read10KBItemsTest;
import com.emc.mongoose.integ.feature.core.Read10MBItemsTest;
import com.emc.mongoose.integ.feature.core.Read200MBItemsTest;
import com.emc.mongoose.integ.feature.core.ReadVerificationTest;
import com.emc.mongoose.integ.feature.core.ReadZeroSizeItemsTest;
import com.emc.mongoose.integ.feature.core.WriteByCountTest;
import com.emc.mongoose.integ.feature.core.WriteByTimeTest;
import com.emc.mongoose.integ.feature.core.WriteRandomSizedItemsTest;
import com.emc.mongoose.integ.feature.core.WriteUsing100ConnTest;
import com.emc.mongoose.integ.feature.core.WriteUsing10ConnTest;
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
})
public class CoreTestSuite {}
