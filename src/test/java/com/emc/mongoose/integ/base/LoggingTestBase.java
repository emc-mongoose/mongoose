package com.emc.mongoose.integ.base;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.integ.tools.StdOutUtil;
import com.emc.mongoose.integ.tools.LogValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import org.junit.AfterClass;
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
	//
	protected static Logger LOG;
	protected static File FILE_LOG_PERF_SUM, FILE_LOG_PERF_AVG,
		FILE_LOG_DATA_ITEMS, FILE_LOG_PERF_TRACE;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		if(System.getProperty(LOG_CONF_PROPERTY_KEY) == null) {
			String fullLogConfFile = Paths
				.get(System.getProperty(USER_DIR_PROPERTY_NAME), Constants.DIR_CONF, LOG_FILE_NAME)
				.toString();
			System.setProperty(LOG_CONF_PROPERTY_KEY, fullLogConfFile);
		}
		LogUtil.init();
		ConfiguredTestBase.setUpClass();
		final String runId = System.getProperty(RunTimeConfig.KEY_RUN_ID);
		LogValidator.removeLogDirectory(runId);
		FILE_LOG_PERF_SUM = LogValidator.getPerfSumFile(runId);
		FILE_LOG_PERF_AVG = LogValidator.getPerfAvgFile(runId);
		FILE_LOG_DATA_ITEMS = LogValidator.getItemsListFile(runId);
		FILE_LOG_PERF_TRACE = LogValidator.getPerfTraceFile(runId);
		final AppConfig appConfig = BasicConfig.CONTEXT_CONFIG.get();
		appConfig.set(RunTimeConfig.KEY_RUN_ID, runId);
		RunTimeConfig.setContext(appConfig);
		LOG = LogManager.getLogger();
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		ConfiguredTestBase.tearDownClass();
		StdOutUtil.reset();
	}
}
