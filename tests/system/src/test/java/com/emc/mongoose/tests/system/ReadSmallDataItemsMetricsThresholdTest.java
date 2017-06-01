package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.HttpStorageDistributedScenarioTestBase;
import com.emc.mongoose.tests.system.util.LogPatterns;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.common.env.DateUtil.FMT_DATE_ISO8601;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 Created by kurila on 01.02.17.
 * 1.2. CLI Arguments Aliasing
 * 2.2.1. Items Input File
 * 2.3.2. Items Output File
 * 7.1. Metrics Periodic Reporting
 * 7.3. Metrics Reporting Triggered by Load Threshold
 * 8.2.1. Create New Items
 * 8.3.1. Read With Disabled Validation
 * 9.2. Default Scenario
 * 9.5.2. Load Job
 * 10.1.2. Two Local Separate Storage Driver Services (at different ports)
 */
public class ReadSmallDataItemsMetricsThresholdTest
extends HttpStorageDistributedScenarioTestBase {
	
	private static final SizeInBytes ITEM_DATA_SIZE = new SizeInBytes("1KB");
	private static final String ITEM_OUTPUT_FILE =
		ReadSmallDataItemsMetricsThresholdTest.class.getSimpleName() + ".csv";
	private static final int LOAD_LIMIT_COUNT = 1_000_000;
	private static final int LOAD_CONCURRENCY = 500;
	private static final double LOAD_THRESHOLD = 0.6;
	
	private static String STD_OUTPUT = null;
	
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		JOB_NAME = ReadSmallDataItemsMetricsThresholdTest.class.getSimpleName();
		ThreadContext.put(KEY_STEP_NAME, JOB_NAME);
		CONFIG_ARGS.add("--item-data-size=" + ITEM_DATA_SIZE.toString());
		try {
			Files.delete(Paths.get(ITEM_OUTPUT_FILE));
		} catch(final Exception ignored) {
		}
		CONFIG_ARGS.add("--item-output-file=" + ITEM_OUTPUT_FILE);
		CONFIG_ARGS.add("--storage-driver-concurrency=" + LOAD_CONCURRENCY);
		CONFIG_ARGS.add("--test-step-limit-count=" + LOAD_LIMIT_COUNT);
		
		HttpStorageDistributedScenarioTestBase.setUpClass();
		SCENARIO.run();
		
		// reinit for read
		SCENARIO.close();
		JOB_NAME = ReadSmallDataItemsMetricsThresholdTest.class.getSimpleName() + "_";
		FileUtils.deleteDirectory(Paths.get(PathUtil.getBaseDir(), "log", JOB_NAME).toFile());
		ThreadContext.put(KEY_STEP_NAME, JOB_NAME);
		LogUtil.init();
		CONFIG_ARGS.remove("--item-data-size=" + ITEM_DATA_SIZE.toString());
		CONFIG_ARGS.add("--item-input-file=" + ITEM_OUTPUT_FILE);
		CONFIG_ARGS.remove("--item-output-file=" + ITEM_OUTPUT_FILE);
		CONFIG_ARGS.remove("--test-step-limit-count=" + LOAD_LIMIT_COUNT);
		CONFIG_ARGS.add("--read");
		CONFIG_ARGS.add("--test-step-metrics-threshold=" + LOAD_THRESHOLD);
		CONFIG.apply(
			CliArgParser.parseArgs(
				CONFIG.getAliasingConfig(), CONFIG_ARGS.toArray(new String[CONFIG_ARGS.size()])
			)
		);
		CONFIG.getItemConfig().getOutputConfig().setFile(null);
		CONFIG.getTestConfig().getStepConfig().getLimitConfig().setCount(0);
		CONFIG.getTestConfig().getStepConfig().setName(JOB_NAME);
		SCENARIO = new JsonScenario(CONFIG, DEFAULT_SCENARIO_PATH.toFile());
		
		final Thread runner = new Thread(
			() -> {
				try {
					STD_OUT_STREAM.startRecording();
					SCENARIO.run();
					STD_OUTPUT = STD_OUT_STREAM.stopRecording();
				} catch(final Throwable t) {
					LogUtil.exception(Level.ERROR, t, "Failed to run the scenario");
				}
			}
		);
		runner.start();
		TimeUnit.MINUTES.timedJoin(runner, 10);
		runner.interrupt();
		LoadJobLogFileManager.flush(JOB_NAME);
		TimeUnit.SECONDS.sleep(20);
	}
	
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		HttpStorageDistributedScenarioTestBase.tearDownClass();
	}
	
	@Test
	public void testMetricsLogFile()
	throws Exception {
		testMetricsLogRecords(
			getMetricsLogRecords(),
			IoType.READ, LOAD_CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			LOAD_LIMIT_COUNT, 0, CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
		);
	}
	
	@Test
	public void testTotalMetricsLogFile()
	throws Exception {
		testTotalMetricsLogRecords(
			getMetricsTotalLogRecords().get(0),
			IoType.READ, LOAD_CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			LOAD_LIMIT_COUNT, 0
		);
	}
	
	@Test public void testMetricsStdout()
	throws Exception {
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.READ, LOAD_CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
		);
	}
	
	@Test
	public void testMedTotalMetricsLogFile()
	throws Exception {
		testTotalMetricsLogRecords(
			getMetricsMedTotalLogRecords().get(0),
			IoType.READ, LOAD_CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			0, 0
		);
	}
	
	@Test public void testFullThrottleConditionMessagesInStdout()
	throws Exception {
		Matcher m = LogPatterns.STD_OUT_LOAD_THRESHOLD_ENTRANCE.matcher(STD_OUTPUT);
		assertTrue(m.find());
		final Date dtEnter = FMT_DATE_ISO8601.parse(m.group("dateTime"));
		final int threshold = Integer.parseInt(m.group("threshold"));
		assertEquals(LOAD_CONCURRENCY * LOAD_THRESHOLD, threshold, 0);
		m = LogPatterns.STD_OUT_LOAD_THRESHOLD_EXIT.matcher(STD_OUTPUT);
		assertTrue(m.find());
		final Date dtExit = FMT_DATE_ISO8601.parse(m.group("dateTime"));
		assertTrue(dtEnter + " should be before " + dtExit, dtEnter.before(dtExit));
	}
}
