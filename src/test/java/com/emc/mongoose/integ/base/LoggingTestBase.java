package com.emc.mongoose.integ.base;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
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
	protected static Logger LOG;
	protected static File FILE_LOG_PERF_SUM, FILE_LOG_PERF_AVG, FILE_LOG_DATA_ITEMS;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ConfiguredTestBase.setUpClass();
		final String runId = System.getProperty(RunTimeConfig.KEY_RUN_ID);
		LogValidator.removeLogDirectory(runId);
		FILE_LOG_PERF_SUM = LogValidator.getPerfSumFile(runId);
		FILE_LOG_PERF_AVG = LogValidator.getPerfAvgFile(runId);
		FILE_LOG_DATA_ITEMS = LogValidator.getItemsListFile(runId);
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_RUN_ID, runId);
		RunTimeConfig.setContext(rtConfig);
		LOG = LogManager.getLogger();
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		ConfiguredTestBase.tearDownClass();
		StdOutInterceptorTestSuite.reset();
	}
}
