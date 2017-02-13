package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.tests.system.base.HttpStorageDistributedScenarioTestBase;
import com.emc.mongoose.tests.system.util.LogPatterns;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static com.emc.mongoose.common.Constants.KEY_JOB_NAME;
import static com.emc.mongoose.common.env.DateUtil.FMT_DATE_ISO8601;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 Created by kurila on 01.02.17.
 Covered use cases:
 * 2.1.1.1.5. Very Big Data Items (100MB-10GB)
 * 7.1. Metrics Periodic Reporting
 * 7.3. Metrics Reporting Triggered by Load Threshold
 * 8.2.1. Create New Items
 * 9.2. Default Scenario
 * 9.5.2. Load Job
 * 10.1.2. Two Local Separate Storage Driver Services (at different ports)
 */
public class CreateBigDataItemsMetricsThresholdTest
extends HttpStorageDistributedScenarioTestBase {
	
	private static final SizeInBytes ITEM_DATA_SIZE = new SizeInBytes("1GB");
	private static final int LOAD_LIMIT_COUNT = 100;
	private static final int LOAD_CONCURRENCY = 100;
	private static final double LOAD_THRESHOLD = 0.98;
	
	private static String STD_OUTPUT = null;
	
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ThreadContext.put(KEY_JOB_NAME, CreateBigDataItemsMetricsThresholdTest.class.getSimpleName());
		CONFIG_ARGS.add("--item-data-size=" + ITEM_DATA_SIZE.toString());
		CONFIG_ARGS.add("--load-concurrency=" + LOAD_CONCURRENCY);
		CONFIG_ARGS.add("--load-limit-count=" + LOAD_LIMIT_COUNT);
		CONFIG_ARGS.add("--load-metrics-threshold=" + LOAD_THRESHOLD);
		HttpStorageDistributedScenarioTestBase.setUpClass();
		final Thread runner = new Thread(
			() -> {
				try {
					STD_OUT_STREAM.startRecording();
					SCENARIO.run();
					STD_OUTPUT = STD_OUT_STREAM.stopRecording();
				} catch(final Throwable t) {
					LogUtil.exception(LOG, Level.ERROR, t, "Failed to run the scenario");
				}
			}
		);
		runner.start();
		TimeUnit.MINUTES.timedJoin(runner, 10);
		LoadJobLogFileManager.flush(JOB_NAME);
		TimeUnit.SECONDS.sleep(1);
	}
	
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		HttpStorageDistributedScenarioTestBase.tearDownClass();
	}
	
	@Test public void testMetricsLogFile()
	throws Exception {
		testMetricsLogRecords(
			getMetricsLogRecords(),
			IoType.CREATE, LOAD_CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			LOAD_LIMIT_COUNT, 0, CONFIG.getLoadConfig().getMetricsConfig().getPeriod()
		);
	}
	
	@Test @Ignore
	public void testTotalMetricsLogFile()
	throws Exception {
		testTotalMetricsLogRecords(
			getMetricsTotalLogRecords().get(0),
			IoType.CREATE, LOAD_CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			LOAD_LIMIT_COUNT, 0
		);
	}
	
	@Test public void testMetricsStdout()
	throws Exception {
		testMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.CREATE, LOAD_CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			CONFIG.getLoadConfig().getMetricsConfig().getPeriod()
		);
	}
	
	@Test public void testMedMetricsLogFile()
	throws Exception {
		testMetricsLogRecords(
			getMetricsMedLogRecords(),
			IoType.CREATE, LOAD_CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			0, 0, CONFIG.getLoadConfig().getMetricsConfig().getPeriod()
		);
	}
	
	@Test public void testMedTotalMetricsLogFile()
	throws Exception {
		testTotalMetricsLogRecords(
			getMetricsMedTotalLogRecords().get(0),
			IoType.CREATE, LOAD_CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
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
		assertTrue(dtEnter.before(dtExit));
	}
}
