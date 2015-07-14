package com.emc.mongoose.integ;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.log.LogUtil;
//
import org.apache.logging.log4j.LogManager;
//
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
//
import java.nio.file.Paths;
/**
 Created by kurila on 14.07.15.
 */
public abstract class LoggingTestBase {
	//
	private final static String
		LOG_CONF_PROPERTY_KEY = "log4j.configurationFile",
		LOG_FILE_NAME = "logging.json",
		USER_DIR_PROPERTY_NAME = "user.dir";
	protected static Logger LOG;
	//
	@BeforeClass
	public static void before()
	throws Exception {
		if (System.getProperty(LOG_CONF_PROPERTY_KEY) == null) {
			String fullLogConfFile = Paths
				.get(System.getProperty(USER_DIR_PROPERTY_NAME), Constants.DIR_CONF, LOG_FILE_NAME)
				.toString();
			System.setProperty(LOG_CONF_PROPERTY_KEY, fullLogConfFile);
		}
		LogUtil.init();
		LOG = LogManager.getLogger();

	}
	//
	@AfterClass
	public static void after()
	throws Exception {
		LogUtil.shutdown();
	}
}
