package com.emc.mongoose.integ;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.integ.integTestTools.IntegConstants;
//
import com.emc.mongoose.storage.mock.impl.Cinderella;
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
	WriteDefaultScenarioIntegTest.class,
	ReadDataItems10BDefaultScenarioIntegTest.class,
	ReadDataItems10KBDefaultScenarioIntegTest.class,
	ReadDataItems10MBDefaultScenarioIntegTest.class,
	ReadDataItems200MBDefaultScenarioIntegTest.class,
	FaildVerificationIntegTest.class,
	Write0BDataItemsScenarioIntegTest.class,
	WriteDataItemWithRandomSizeIntegTest.class,
	SingleWriteScenarioWith10ConcurrentConnectionsIntegTest.class,
	SingleWriteScenarioWith100ConcurrentConnectionsIntegTest.class,
})
public class IntegTestSuite {

	private static Thread wsMockThread;

	@BeforeClass
	public static void startCinderella()
	throws Exception {
		// If tests run from the IDEA full logging file must be set
		final String fullLogConfFile = Paths
			.get(System.getProperty(IntegConstants.USER_DIR_PROPERTY_NAME), Constants.DIR_CONF, IntegConstants.LOG_FILE_NAME)
			.toString();
		System.setProperty(IntegConstants.LOG_CONF_PROPERTY_KEY, fullLogConfFile);
		//
		LogUtil.init();
		RunTimeConfig.initContext();
		// If tests run from the IDEA full logging file must be set
		wsMockThread = new Thread(new Cinderella<>(RunTimeConfig.getContext()), "cinderella");
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
