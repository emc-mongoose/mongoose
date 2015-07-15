package com.emc.mongoose.integ;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.integ.core.chain.CRUDSequentialScenarioIntegTest;
import com.emc.mongoose.integ.core.chain.CRUDSimultaneousScenarioIntegTest;
import com.emc.mongoose.integ.core.chain.CustomChainScenarioIntegTest;
import com.emc.mongoose.integ.core.chain.DefaultChainScenarioIntegTest;
import com.emc.mongoose.integ.core.single.DefaultWriteTest;
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
import com.emc.mongoose.integ.tools.TestConstants;
//
import com.emc.mongoose.storage.mock.impl.web.Cinderella;
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
	CustomChainScenarioIntegTest.class,
	CRUDSequentialScenarioIntegTest.class,
	CRUDSimultaneousScenarioIntegTest.class,
	DefaultChainScenarioIntegTest.class
})
public class IntegrationTestSuite {

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
