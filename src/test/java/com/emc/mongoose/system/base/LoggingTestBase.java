package com.emc.mongoose.system.base;
//
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.system.tools.StdOutUtil;
import com.emc.mongoose.system.tools.LogValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import org.junit.AfterClass;
import org.junit.BeforeClass;
//
import java.io.File;
import java.nio.file.Paths;

import static com.emc.mongoose.common.conf.AppConfig.KEY_RUN_ID;

/**
 Created by andrey on 13.08.15.
 */
public abstract class LoggingTestBase
extends ConfiguredTestBase {
	//
	private final static String
		LOG_CONF_PROPERTY_KEY = "log4j.configurationFile",
		LOG_FILE_NAME = "logging.json";
	//
	protected static Logger LOG;
	protected static File FILE_LOG_PERF_SUM, FILE_LOG_PERF_MED, FILE_LOG_PERF_AVG,
		FILE_LOG_DATA_ITEMS, FILE_LOG_PERF_TRACE;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		try {
			if(System.getProperty(LOG_CONF_PROPERTY_KEY) == null) {
				final String fullLogConfFile = Paths.get(
					BasicConfig.getWorkingDir(), Constants.DIR_CONF, LOG_FILE_NAME
				).toString();
				System.setProperty(LOG_CONF_PROPERTY_KEY, fullLogConfFile);
			}
			final String runId = System.setProperty(KEY_RUN_ID, LogUtil.newRunId());
			LogUtil.init();
			ConfiguredTestBase.setUpClass();
			LogValidator.removeLogDirectory(runId);
			FILE_LOG_PERF_SUM = LogValidator.getPerfSumFile(runId);
			FILE_LOG_PERF_MED = LogValidator.getPerfMedFile(runId);
			FILE_LOG_PERF_AVG = LogValidator.getPerfAvgFile(runId);
			FILE_LOG_DATA_ITEMS = LogValidator.getItemsListFile(runId);
			FILE_LOG_PERF_TRACE = LogValidator.getPerfTraceFile(runId);
			BasicConfig.THREAD_CONTEXT.get().setRunId(runId);
			LOG = LogManager.getLogger();
		} catch(final NullPointerException e) {
			e.printStackTrace(System.out);
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		ConfiguredTestBase.tearDownClass();
		StdOutUtil.reset();
	}
}
