package com.emc.mongoose.integ.suite;

import com.emc.mongoose.integ.core.rampup.CustomRampupTest;
import com.emc.mongoose.integ.core.rampup.DefaultRampupTest;

import com.emc.mongoose.integ.core.chain.CRUDSequentialScenarioIntegTest;
import com.emc.mongoose.integ.core.chain.CRUDSimultaneousScenarioIntegTest;
import com.emc.mongoose.integ.core.chain.CustomChainScenarioIntegTest;
import com.emc.mongoose.integ.core.chain.DefaultChainScenarioIntegTest;

import com.emc.mongoose.integ.core.single.DefaultWriteTest;
import com.emc.mongoose.integ.core.single.InfiniteWriteTest;
import com.emc.mongoose.integ.core.single.Read10BItemsTest;
import com.emc.mongoose.integ.core.single.Read10KBItemsTest;
import com.emc.mongoose.integ.core.single.Read10MBItemsTest;
import com.emc.mongoose.integ.core.single.Read200MBItemsTest;
import com.emc.mongoose.integ.core.single.ReadVerificationTest;
import com.emc.mongoose.integ.core.single.ReadZeroSizeItemsTest;
import com.emc.mongoose.integ.core.single.WriteByCountTest;
import com.emc.mongoose.integ.core.single.WriteByTimeTest;
import com.emc.mongoose.integ.core.single.WriteRandomSizedItemsTest;
import com.emc.mongoose.integ.core.single.WriteUsing100ConnTest;
import com.emc.mongoose.integ.core.single.WriteUsing10ConnTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Created by olga on 03.07.15.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	/*DefaultWriteTest.class,
	Read10BItemsTest.class,
	Read10KBItemsTest.class,
	Read10MBItemsTest.class,
	Read200MBItemsTest.class,
	ReadVerificationTest.class,
	ReadZeroSizeItemsTest.class,
	WriteByTimeTest.class,
	WriteByCountTest.class,
	WriteRandomSizedItemsTest.class,
	WriteUsing10ConnTest.class,
	WriteUsing100ConnTest.class,*/
	/*CRUDSequentialScenarioIntegTest.class,*/
	CRUDSimultaneousScenarioIntegTest.class,
	/*CustomChainScenarioIntegTest.class,*/
	/*DefaultChainScenarioIntegTest.class,
	CustomRampupTest.class,
	DefaultRampupTest.class,*/
	InfiniteWriteTest.class
})
public class CoreTestSuite {
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		WSMockTestSuite.setUpClass();
	}
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		WSMockTestSuite.tearDownClass();
	}
}
