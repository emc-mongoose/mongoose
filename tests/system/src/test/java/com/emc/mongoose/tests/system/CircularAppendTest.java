package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.tests.system.base.HttpStorageDistributedScenarioTestBase;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.Frequency;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.common.Constants.KEY_JOB_NAME;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 Created by andrey on 06.02.17.
 * 2.1.1.1.2. Small Data Items (16KB)
 * 2.2.1. Items Input File
 * 2.3.2. Items Output File
 * 4.2. Small Concurrency Level (2-10)
 * 5. Circularity
 * 6.2.2. Limit Load Job by Processed Item Count
 * 8.2.1. Create New Items
 * 8.4.3.4. Append
 * 9.3. Custom Scenario File
 * 9.4.1. Override Default Configuration in the Scenario
 * 9.5.5. Sequential Job
 * 10.1.2. Two Local Separate Storage Driver Services (at different ports)
 */
public class CircularAppendTest
extends HttpStorageDistributedScenarioTestBase {

	private static final Path SCENARIO_PATH = Paths.get(
		getBaseDir(), DIR_SCENARIO, "circular", "append.json"
	);
	private static final SizeInBytes EXPECTED_INITIAL_SIZE = new SizeInBytes("16KB");
	private static final SizeInBytes EXPECTED_FINAL_SIZE = new SizeInBytes("32KB-640KB");
	private static final int EXPECTED_CONCURRENCY = 10;
	private static final long EXPECTED_COUNT = 1000;
	private static final String ITEM_OUTPUT_FILE_0 = "circular-append-before.csv";
	private static final String ITEM_OUTPUT_FILE_1 = "circular-append-after.csv";

	private static String STD_OUTPUT;
	private static boolean FINISHED_IN_TIME;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		JOB_NAME = CircularAppendTest.class.getSimpleName();
		try {
			Files.delete(Paths.get(ITEM_OUTPUT_FILE_0));
			Files.delete(Paths.get(ITEM_OUTPUT_FILE_1));
		} catch(final Exception ignored) {
		}
		ThreadContext.put(KEY_JOB_NAME, JOB_NAME);
		CONFIG_ARGS.add("--scenario-file=" + SCENARIO_PATH.toString());
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
		TimeUnit.MINUTES.timedJoin(runner, 65);
		FINISHED_IN_TIME = !runner.isAlive();
		LoadJobLogFileManager.flush(JOB_NAME);
		TimeUnit.SECONDS.sleep(1);
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

	@Test public void testMetricsLogFile()
	throws Exception {
		final List<CSVRecord> metricsLogRecords = getMetricsLogRecords();
		assertTrue(
			"There should be more than 0 metrics records in the log file",
			metricsLogRecords.size() > 0
		);
		testMetricsLogRecords(
			metricsLogRecords, IoType.UPDATE, EXPECTED_CONCURRENCY, STORAGE_DRIVERS_COUNT,
			EXPECTED_INITIAL_SIZE, 31000, 0, CONFIG.getLoadConfig().getMetricsConfig().getPeriod()
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
			totalMetrcisLogRecords.get(0), IoType.UPDATE, EXPECTED_CONCURRENCY, STORAGE_DRIVERS_COUNT,
			EXPECTED_INITIAL_SIZE, 0, 0
		);
	}

	@Test public void testMetricsStdout()
	throws Exception {
		testMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.UPDATE, EXPECTED_CONCURRENCY, STORAGE_DRIVERS_COUNT, EXPECTED_INITIAL_SIZE,
			CONFIG.getLoadConfig().getMetricsConfig().getPeriod()
		);
	}

	@Test public void testIoTraceLogFile()
	throws Exception {
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertTrue(
			"There should be more than " + EXPECTED_COUNT + " records in the I/O trace log file",
			EXPECTED_COUNT < ioTraceRecords.size()
		);
		for(final CSVRecord ioTraceRecord : ioTraceRecords) {
			testIoTraceRecord(ioTraceRecord, IoType.UPDATE.ordinal(), EXPECTED_INITIAL_SIZE);
		}
	}

	@Test public void testUpdatedItemsOutputFile()
	throws Exception {
		final List<CSVRecord> items = new ArrayList<>();
		try(final BufferedReader br = new BufferedReader(new FileReader(ITEM_OUTPUT_FILE_1))) {
			final CSVParser csvParser = CSVFormat.RFC4180.parse(br);
			for(final CSVRecord csvRecord : csvParser) {
				items.add(csvRecord);
			}
		}
		final int itemIdRadix = CONFIG.getItemConfig().getNamingConfig().getRadix();
		final Frequency freq = new Frequency();
		String itemPath, itemId;
		long itemOffset;
		long itemSize;
		for(final CSVRecord itemRec : items) {
			itemPath = itemRec.get(0);
			itemId = itemPath.substring(itemPath.lastIndexOf('/') + 1);
			itemOffset = Long.parseLong(itemRec.get(1), 0x10);
			assertEquals(Long.parseLong(itemId, itemIdRadix), itemOffset);
			freq.addValue(itemOffset);
			itemSize = Long.parseLong(itemRec.get(2));
			assertTrue(EXPECTED_FINAL_SIZE.getMin() <= itemSize && itemSize <= EXPECTED_FINAL_SIZE.getMax());
			assertEquals("0/0", itemRec.get(3));
		}
		assertEquals(EXPECTED_COUNT, freq.getUniqueCount());
	}
}
