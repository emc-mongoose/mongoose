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
import org.junit.Assert;
import org.junit.BeforeClass;

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
 * 2.1.1.1.3. Intermediate Size Data Items (100KB-10MB)
 * 2.2.1. Items Input File
 * 2.2.3.1. Random Item Ids
 * 2.3.2. Items Output File
 * 2.3.3.1. Constant Items Destination Path
 * 4.1. Default Concurrency Level (1)
 * 6.1. Load Jobs Naming
 * 6.2.2. Limit Load Job by Processed Item Count
 * 8.2.1. Create New Items
 * 8.3.2. Read With Enabled Validation
 * 8.3.3.2.4. Read Multiple Fixed Ranges
 * 9.3. Custom Scenario File
 * 9.4.3. Reusing The Items in the Scenario
 * 9.5.2. Load Job
 * 9.5.5. Sequential Job
 * 10.1.2. Two Local Separate Storage Driver Services (at different ports)
 */

public class TlsReadUpdatedMultipleRandomRangesTest
extends HttpStorageDistributedScenarioTestBase {

	private static final Path SCENARIO_PATH = Paths.get(
		getBaseDir(), DIR_SCENARIO, "partial", "read-multiple-random-ranges-updated.json"
	);
	private static final SizeInBytes EXPECTED_ITEM_DATA_SIZE = new SizeInBytes("1-1KB");
	private static final int EXPECTED_CONCURRENCY = 4;
	private static final long EXPECTED_COUNT = 1000;
	private static final String ITEM_OUTPUT_FILE_0 = "read-multiple-random-ranges-0.csv";
	private static final String ITEM_OUTPUT_FILE_1 = "read-multiple-random-ranges-1.csv";

	private static String STD_OUTPUT;
	private static boolean FINISHED_IN_TIME;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		STEP_NAME = TlsReadUpdatedMultipleRandomRangesTest.class.getSimpleName();
		try {
			Files.delete(Paths.get(ITEM_OUTPUT_FILE_1));
		} catch(final Exception ignored) {
		}
		try {
			Files.delete(Paths.get(ITEM_OUTPUT_FILE_0));
		} catch(final Exception ignored) {
		}
		ThreadContext.put(KEY_STEP_NAME, STEP_NAME);
		CONFIG_ARGS.add("--item-data-verify=true");
		CONFIG_ARGS.add("--storage-driver-concurrency=" + EXPECTED_CONCURRENCY);
		CONFIG_ARGS.add("--storage-net-ssl=true");
		CONFIG_ARGS.add("--test-scenario-file=" + SCENARIO_PATH.toString());
		HttpStorageDistributedScenarioTestBase.setUpClass();
		final Thread runner = new Thread(
			() -> {
				try {
					STD_OUT_STREAM.startRecording();
					SCENARIO.run();
					STD_OUTPUT = STD_OUT_STREAM.stopRecordingAndGet();
				} catch(final Throwable t) {
					LogUtil.exception(Level.ERROR, t, "Failed to run the scenario");
				}
			}
		);
		runner.start();
		TimeUnit.MINUTES.timedJoin(runner, 2);
		FINISHED_IN_TIME = !runner.isAlive();
		runner.interrupt();
		LoadJobLogFileManager.flush(STEP_NAME);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		HttpStorageDistributedScenarioTestBase.tearDownClass();
	}


	public final void testFinishedInTime() {
		assertTrue(FINISHED_IN_TIME);
	}


	public final void testMetricsLogFile()
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

	public void testMetricsStdout()
	throws Exception {
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.READ, EXPECTED_CONCURRENCY, STORAGE_DRIVERS_COUNT, EXPECTED_ITEM_DATA_SIZE,
			CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
		);
	}

	public void testIoTraceLogFile()
	throws Exception {
		TimeUnit.SECONDS.sleep(15);
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertEquals(
			"There should be " + EXPECTED_COUNT + " records in the I/O trace log file",
			EXPECTED_COUNT, ioTraceRecords.size()
		);
		for(final CSVRecord ioTraceRecord : ioTraceRecords) {
			testIoTraceRecord(ioTraceRecord, IoType.READ.ordinal(), EXPECTED_ITEM_DATA_SIZE);
		}
	}

	public void testTlsEnableLogged()
	throws Exception {
		final List<String> msgLogLines = getMessageLogLines();
		int msgCount = 0;
		for(final String msgLogLine : msgLogLines) {
			if(msgLogLine.contains(STEP_NAME + ": SSL/TLS is enabled for the channel")) {
				msgCount ++;
			}
		}
		// 3 steps + additional bucket checking/creating connections
		Assert.assertTrue(3 * STORAGE_DRIVERS_COUNT * EXPECTED_CONCURRENCY <= msgCount);
	}
}
