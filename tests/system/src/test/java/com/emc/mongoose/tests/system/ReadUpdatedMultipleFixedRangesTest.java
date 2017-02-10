package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.tests.system.base.HttpStorageDistributedScenarioTestBase;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.common.Constants.KEY_JOB_NAME;
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
 * 10.3. Filesystem Storage Driver
 */
public class ReadUpdatedMultipleFixedRangesTest
extends HttpStorageDistributedScenarioTestBase {

	private static final Path SCENARIO_PATH = Paths.get(
		getBaseDir(), DIR_SCENARIO, "partial", "read-multiple-fixed-ranges-updated.json"
	);
	private static final SizeInBytes EXPECTED_ITEM_DATA_SIZE = new SizeInBytes(
		(34 - 12 + 1) + (78 - 56 + 1) + (1024 - 910 + 1)
	);
	private static final int EXPECTED_CONCURRENCY = 1;
	private static final long EXPECTED_COUNT = 1000;
	private static final String ITEM_OUTPUT_FILE_0 = "read-multiple-fixed-ranges-0.csv";
	private static final String ITEM_OUTPUT_FILE_1 = "read-multiple-fixed-ranges-1.csv";

	private static String STD_OUTPUT;
	private static boolean FINISHED_IN_TIME;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		JOB_NAME = ReadUpdatedMultipleFixedRangesTest.class.getSimpleName();
		try {
			Files.delete(Paths.get(ITEM_OUTPUT_FILE_1));
		} catch(final Exception ignored) {
		}
		try {
			Files.delete(Paths.get(ITEM_OUTPUT_FILE_0));
		} catch(final Exception ignored) {
		}
		ThreadContext.put(KEY_JOB_NAME, JOB_NAME);
		CONFIG_ARGS.add("--scenario-file=" + SCENARIO_PATH.toString());
		CONFIG_ARGS.add("--item-data-verify=true");
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
		TimeUnit.MINUTES.timedJoin(runner, 20000);
		FINISHED_IN_TIME = !runner.isAlive();
		LoadJobLogFileManager.flush(JOB_NAME);
		TimeUnit.SECONDS.sleep(20);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		HttpStorageDistributedScenarioTestBase.tearDownClass();
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
			EXPECTED_COUNT, 0, CONFIG.getLoadConfig().getMetricsConfig().getPeriod()
		);
	}

	@Test public void testTotalMetricsLogFile()
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
		testMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.READ, EXPECTED_CONCURRENCY, STORAGE_DRIVERS_COUNT, EXPECTED_ITEM_DATA_SIZE,
			CONFIG.getLoadConfig().getMetricsConfig().getPeriod()
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
