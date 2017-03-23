
package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.tests.system.base.FileStorageDistributedScenarioTestBase;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
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
 Created by andrey on 07.02.17.
 Covered use cases:
 * 2.1.1.1.2. Small Data Items (1KB-100KB)
 * 2.2.1. Items Input File
 * 2.2.3.1. Random Item Ids
 * 2.3.2. Items Output File
 * 2.3.3.1. Constant Items Destination Path
 * 4.1. Default Concurrency Level (1)
 * 6.1. Load Jobs Naming
 * 6.2.2. Limit Load Job by Processed Item Count
 * 8.2.1. Create New Items
 * 8.3.2. Read With Enabled Validation
 * 8.3.3.1.2. Multiple Random Byte Ranges Read
 * 9.3. Custom Scenario File
 * 9.4.3. Reusing The Items in the Scenario
 * 9.5.2. Load Job
 * 9.5.5. Sequential Job
 * 10.1.2. Two Local Separate Storage Driver Services (at different ports)
 * 10.3. Filesystem Storage Driver
 */
public class ReadMultipleRandomFileRangesTest
extends FileStorageDistributedScenarioTestBase {

	private static final Path SCENARIO_PATH = Paths.get(
		getBaseDir(), DIR_SCENARIO, "partial", "read-multiple-random-ranges.json"
	);
	private static final SizeInBytes EXPECTED_ITEM_DATA_SIZE = new SizeInBytes("1-1KB");
	private static final int EXPECTED_CONCURRENCY = 1;
	private static final long EXPECTED_COUNT = 1000;
	private static final String ITEM_OUTPUT_FILE = "read-multiple-random-ranges.csv";
	private static final String ITEM_OUTPUT_PATH = "/tmp/read-multiple-random-ranges";

	private static String STD_OUTPUT;
	private static boolean FINISHED_IN_TIME;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		JOB_NAME = ReadMultipleRandomFileRangesTest.class.getSimpleName();
		try {
			Files.delete(Paths.get(ITEM_OUTPUT_FILE));
			FileUtils.deleteDirectory(new File(ITEM_OUTPUT_PATH));
		} catch(final Exception ignored) {
		}
		ThreadContext.put(KEY_STEP_NAME, JOB_NAME);
		CONFIG_ARGS.add("--test-scenario-file=" + SCENARIO_PATH.toString());
		CONFIG_ARGS.add("--item-output-path=" + ITEM_OUTPUT_PATH);
		CONFIG_ARGS.add("--item-data-verify=true");
		FileStorageDistributedScenarioTestBase.setUpClass();
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
		TimeUnit.MINUTES.timedJoin(runner, 1);
		FINISHED_IN_TIME = !runner.isAlive();
		runner.interrupt();
		LoadJobLogFileManager.flush(JOB_NAME);
		TimeUnit.SECONDS.sleep(10);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		FileStorageDistributedScenarioTestBase.tearDownClass();
	}

	@Test
	public final void testFinishedInTime() {
		assertTrue(FINISHED_IN_TIME);
	}

	@Test public void testMetricsLogFile()
	throws Exception {
		final List<CSVRecord> metricsLogRecords = getMetricsLogRecords();
		assertTrue(
			"There should be more than 0 metrics records in the log file",
			metricsLogRecords.size() > 0
		);
		testMetricsLogRecords(
			metricsLogRecords, IoType.READ, EXPECTED_CONCURRENCY, STORAGE_DRIVERS_COUNT,
			EXPECTED_ITEM_DATA_SIZE,
			EXPECTED_COUNT, 0, CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
		);
	}

	@Test @Ignore
	public void testTotalMetricsLogFile()
	throws Exception {
		final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords();
		assertEquals(
			"There should be 1 total metrics records in the log file", 1,
			totalMetrcisLogRecords.size()
		);
		testTotalMetricsLogRecords(
			totalMetrcisLogRecords.get(0), IoType.READ, EXPECTED_CONCURRENCY, STORAGE_DRIVERS_COUNT,
			EXPECTED_ITEM_DATA_SIZE, EXPECTED_COUNT, 0
		);
	}

	@Test public void testMetricsStdout()
	throws Exception {
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.READ, EXPECTED_CONCURRENCY, STORAGE_DRIVERS_COUNT, EXPECTED_ITEM_DATA_SIZE,
			CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
		);
	}

	@Test public void testIoTraceLogFile()
	throws Exception {
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertEquals(
			"There should be " + EXPECTED_COUNT + " records in the I/O trace log file",
			EXPECTED_COUNT, ioTraceRecords.size()
		);
		for(final CSVRecord ioTraceRecord : ioTraceRecords) {
			testIoTraceRecord(ioTraceRecord, IoType.READ.ordinal(), EXPECTED_ITEM_DATA_SIZE);
		}
	}
}
