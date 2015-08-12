package com.emc.mongoose.integ.suite;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.log.LogUtil;
//
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
//
import java.nio.file.Paths;
/**
 Created by kurila on 14.07.15.
 */
@RunWith(Suite.class)
public abstract class LoggingTestSuite {
	//
	private final static String
		LOG_CONF_PROPERTY_KEY = "log4j.configurationFile",
		LOG_FILE_NAME = "logging.json",
		USER_DIR_PROPERTY_NAME = "user.dir";
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		if (System.getProperty(LOG_CONF_PROPERTY_KEY) == null) {
			String fullLogConfFile = Paths
				.get(System.getProperty(USER_DIR_PROPERTY_NAME), Constants.DIR_CONF, LOG_FILE_NAME)
				.toString();
			System.setProperty(LOG_CONF_PROPERTY_KEY, fullLogConfFile);
		}
		LogUtil.init();
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		//LogUtil.shutdown();
	}
}
