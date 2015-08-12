package com.emc.mongoose.integ.base;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.integ.tools.LogParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
//
import java.io.File;
import java.nio.file.Paths;
/**
 Created by andrey on 13.08.15.
 */
public abstract class LoggingTestBase
extends ConfiguredTestBase {
	//
	private final static String
		LOG_CONF_PROPERTY_KEY = "log4j.configurationFile",
		LOG_FILE_NAME = "logging.json",
		USER_DIR_PROPERTY_NAME = "user.dir";
	protected static Logger LOG;
	//
	protected File fileLogPerfSum, fileLogPerfAvg;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ConfiguredTestBase.setUpClass();
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
	public static void tearDownClass()
	throws Exception {
		LogUtil.shutdown();
		ConfiguredTestBase.tearDownClass();
	}
	//
	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		final String runId = getClass().getCanonicalName();
		LogParser.removeLogDirectory(runId);
		RT_CONFIG.set(RunTimeConfig.KEY_RUN_ID, runId);
		fileLogPerfSum = LogParser.getPerfSumFile(runId);
		if(fileLogPerfSum.exists()) {
			fileLogPerfSum.delete();
		}
		fileLogPerfAvg = LogParser.getPerfAvgFile(runId);
		if(fileLogPerfAvg.exists()) {
			fileLogPerfAvg.delete();
		}
	}
	//
	@After
	public void tearDown()
	throws Exception {
		super.tearDown();
	}
}
