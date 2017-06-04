package com.emc.mongoose.tests.system.deprecated;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.tests.system.base.deprecated.HttpStorageDistributedScenarioTestBase;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 Created by andrey on 05.03.17.
 Covered use cases:
 * 2.1.1.1.5. Very Big Data Items (100MB-10GB)
 * 4.2. Small Concurrency Level (2-10)
 * 6.1. Load Jobs Naming
 * 6.2.2. Limit Load Job by Processed Item Count
 * 7.1. Metrics Periodic Reporting
 * 8.2.1. Create New Items
 * 9.3. Custom Scenario File
 * 9.4.1. Override Default Configuration in the Scenario
 * 9.5.2. Load Job
 * 10.1.2. Many Local Separate Storage Driver Services (at different ports)
 * 10.2.2. Destination Path Precondition Hook
 * 10.4.4. I/O Buffer Size Adjustment for Optimal Performance
 * 10.4.5.4.3. Create Dynamic Large Objects
 */
@Ignore
public class SwiftCreateDloTest
extends HttpStorageDistributedScenarioTestBase {

	private static final Path SCENARIO_PATH = Paths.get(
		getBaseDir(), DIR_SCENARIO, "swift", "create-dlo.json"
	);
	private static final int EXPECTED_CONCURRENCY = 10;
	private static final long EXPECTED_COUNT = 100;
	private static final SizeInBytes EXPECTED_SIZE = new SizeInBytes("1GB");
	private static final SizeInBytes EXPECTED_PART_SIZE = new SizeInBytes("64MB");

	private static boolean FINISHED_IN_TIME;
	private static String STD_OUTPUT;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		JOB_NAME = SwiftCreateDloTest.class.getSimpleName();
		ThreadContext.put(KEY_STEP_NAME, JOB_NAME);
		CONFIG_ARGS.add("--test-scenario-file=" + SCENARIO_PATH.toString());
		HttpStorageDistributedScenarioTestBase.setUpClass();
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
		TimeUnit.SECONDS.timedJoin(runner, 300);
		FINISHED_IN_TIME = !runner.isAlive();
		runner.interrupt();
		LoadJobLogFileManager.flush(JOB_NAME);
		TimeUnit.SECONDS.sleep(10);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		HttpStorageDistributedScenarioTestBase.tearDownClass();
	}

	@Test
	public void testFinishedInTime() {
		assertTrue("Scenario didn't finished in time", FINISHED_IN_TIME);
	}

	@Test
	public void testMetricsLogFile()
	throws Exception {
		final List<CSVRecord> metricsLogRecords = getMetricsLogRecords();
		assertTrue(
			"There should be more than 0 metrics records in the log file",
			metricsLogRecords.size() > 0
		);
	}

	@Test
	public void testTotalMetricsLogFile()
	throws Exception {
		final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords();
		assertEquals(
			"There should be 1 total metrics records in the log file", 1,
			totalMetrcisLogRecords.size()
		);
		testTotalMetricsLogRecords(
			totalMetrcisLogRecords.get(0), IoType.CREATE, EXPECTED_CONCURRENCY,
			STORAGE_DRIVERS_COUNT, EXPECTED_SIZE, EXPECTED_COUNT, 0
		);
	}

	@Test
	public void testMetricsStdout()
	throws Exception {
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.CREATE, EXPECTED_CONCURRENCY, STORAGE_DRIVERS_COUNT, EXPECTED_SIZE,
			CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
		);
	}

	@Test
	public void testIoTraceLogFile()
	throws Exception {
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertTrue(
			"There should be more than " + EXPECTED_COUNT +
				" records in the I/O trace log file, but got: " + ioTraceRecords.size(),
			EXPECTED_COUNT < ioTraceRecords.size()
		);
		final SizeInBytes ZERO_SIZE = new SizeInBytes(0);
		final SizeInBytes TAIL_PART_SIZE = new SizeInBytes(
			EXPECTED_SIZE.get() % EXPECTED_PART_SIZE.get()
		);
		for(final CSVRecord ioTraceRecord : ioTraceRecords) {
			try {
				testIoTraceRecord(ioTraceRecord, IoType.CREATE.ordinal(), ZERO_SIZE);
			} catch(final AssertionError e) {
				try {
					testIoTraceRecord(ioTraceRecord, IoType.CREATE.ordinal(), EXPECTED_PART_SIZE);
				} catch(final AssertionError ee) {
					testIoTraceRecord(ioTraceRecord, IoType.CREATE.ordinal(), TAIL_PART_SIZE);
				}
			}
		}
	}
}
