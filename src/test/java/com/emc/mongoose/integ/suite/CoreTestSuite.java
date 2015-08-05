package com.emc.mongoose.integ.suite;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.integ.core.api.atmos.AtmosMultiRangeUpdateTest;
import com.emc.mongoose.integ.core.api.s3.S3ReadUsingBucketListingTest;
import com.emc.mongoose.integ.core.api.s3.S3UsePreExistingBucketTest;
import com.emc.mongoose.integ.core.rampup.CustomRampupTest;
import com.emc.mongoose.integ.core.rampup.DefaultRampupTest;
import com.emc.mongoose.integ.core.single.*;
import com.emc.mongoose.storage.mock.impl.web.Cinderella;
//
import com.emc.mongoose.integ.core.chain.CRUDSequentialScenarioIntegTest;
import com.emc.mongoose.integ.core.chain.CRUDSimultaneousScenarioIntegTest;
import com.emc.mongoose.integ.core.chain.CustomChainScenarioIntegTest;
import com.emc.mongoose.integ.core.chain.DefaultChainScenarioIntegTest;
import com.emc.mongoose.integ.tools.TestConstants;
//
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
//
import java.nio.file.Paths;

/**
 * Created by olga on 03.07.15.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	AtmosMultiRangeUpdateTest.class,
	S3UsePreExistingBucketTest.class,
	S3ReadUsingBucketListingTest.class,
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
	CustomChainScenarioIntegTest.class,
	CRUDSequentialScenarioIntegTest.class,
	CRUDSimultaneousScenarioIntegTest.class,
	DefaultChainScenarioIntegTest.class,
	CustomRampupTest.class,
	DefaultRampupTest.class,
	InfiniteWriteTest.class*/
})
public class CoreTestSuite {

	private static Thread wsMockThread;

	@BeforeClass
	public static void startCinderella()
	throws Exception {
		// If tests run from the IDEA full logging file must be set
		final String fullLogConfFile = Paths
			.get(System.getProperty(TestConstants.USER_DIR_PROPERTY_NAME), Constants.DIR_CONF, TestConstants.LOG_FILE_NAME)
			.toString();
		System.setProperty(TestConstants.LOG_CONF_PROPERTY_KEY, fullLogConfFile);
		//
		LogUtil.init();
		RunTimeConfig.initContext();
		// If tests run from the IDEA full logging file must be set
		wsMockThread = new Thread(new Cinderella(RunTimeConfig.getContext()), "cinderella");
		wsMockThread.setDaemon(true);
		wsMockThread.start();
	}

	@AfterClass
	public static void interruptCinderella()
	throws Exception {
		if (!wsMockThread.isInterrupted()) {
			wsMockThread.interrupt();
		}
	}
}
