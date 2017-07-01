package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.common.Constants.KEY_TEST_STEP_ID;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

/**
 Created by andrey on 06.02.17.
 Covered use cases:
 * 2.1.1.1.3. Intermediate Size Data Items (100KB-10MB)
 * 2.2.1. Items Input File
 * 2.3.2. Items Output File
 * 4.2. Small Concurrency Level (2-10)
 * 5. Circularity
 * 6.2.2. Limit Load Job by Processed Item Count
 * 6.2.5. Limit Load Job by Time
 * 8.2.1. Create New Items
 * 8.3.1. Read With Disabled Validation
 * 9.3. Custom Scenario File
 * 9.4.1. Override Default Configuration in the Scenario
 * 9.5.5. Sequential Job
 * 10.1.2. Two Local Separate Storage Driver Services (at different ports)
 */

public class CircularReadSingleItemTest
extends EnvConfiguredScenarioTestBase {

	static {
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("atmos", "swift"));
		EXCLUDE_PARAMS.put(
			KEY_ENV_ITEM_DATA_SIZE,
			Arrays.asList(new SizeInBytes("100MB"), new SizeInBytes("10GB"))
		);
		STEP_NAME = CircularReadSingleItemTest.class.getSimpleName();
		SCENARIO_PATH = Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "CircularReadSingleItem.json"
		);
	}

	private static final String ITEM_OUTPUT_FILE =
		CircularReadSingleItemTest.class.getSimpleName() + ".csv";

	private static String STD_OUTPUT;
	private static boolean FINISHED_IN_TIME;
	private static String ITEM_OUTPUT_PATH;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		STEP_NAME = CircularReadSingleItemTest.class.getSimpleName();
		ThreadContext.put(KEY_TEST_STEP_ID, STEP_NAME);
		CONFIG_ARGS.add("--storage-net-http-namespace=ns1");
		EnvConfiguredScenarioTestBase.setUpClass();
		if(SKIP_FLAG) {
			return;
		}
		if(STORAGE_DRIVER_TYPE.equals(STORAGE_TYPE_FS)) {
			ITEM_OUTPUT_PATH = Paths.get(
				Paths.get(PathUtil.getBaseDir()).getParent().toString(), STEP_NAME
			).toString();
			CONFIG.getItemConfig().getOutputConfig().setPath(ITEM_OUTPUT_PATH);
		}
		SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
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
		TimeUnit.MINUTES.timedJoin(runner, 65); // 1m + up to 5s for the precondition job
		FINISHED_IN_TIME = !runner.isAlive();
		runner.interrupt();
		LogUtil.flushAll();
		TimeUnit.SECONDS.sleep(10);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		if(! SKIP_FLAG) {
			if(STORAGE_DRIVER_TYPE.equals(STORAGE_TYPE_FS)) {
				try {
					DirWithManyFilesDeleter.deleteExternal(ITEM_OUTPUT_PATH);
				} catch(final Exception e) {
					e.printStackTrace(System.err);
				}
			}
		}
		EnvConfiguredScenarioTestBase.tearDownClass();
	}

	@Test
	public void testFinishedInTime() {
		assumeFalse(SKIP_FLAG);
		assertTrue("Scenario didn't finished in time", FINISHED_IN_TIME);
	}

	@Test
	public void testMetricsLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> metricsLogRecords = getMetricsLogRecords();
		assertTrue(
			"There should be more than 2 metrics records in the log file",
			metricsLogRecords.size() > 1
		);
		testMetricsLogRecords(
			metricsLogRecords, IoType.READ, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			0, 60, CONFIG.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
	}

	@Test
	public void testTotalMetricsLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords();
		assertEquals(
			"There should be 1 total metrics records in the log file", 1,
			totalMetrcisLogRecords.size()
		);
		testTotalMetricsLogRecord(
			totalMetrcisLogRecords.get(0), IoType.READ, CONCURRENCY, STORAGE_DRIVERS_COUNT,
			ITEM_DATA_SIZE, 0, 60
		);
	}

	@Test
	public void testMetricsStdout()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.CREATE, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			CONFIG.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
	}

	@Test
	public void testIoTraceLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertTrue(
			"There should be more than 1 record in the I/O trace log file",
			ioTraceRecords.size() > 1
		);
		for(final CSVRecord ioTraceRecord : ioTraceRecords) {
			testIoTraceRecord(ioTraceRecord, IoType.READ.ordinal(), ITEM_DATA_SIZE);
		}
	}

	@Test
	public void testItemsOutputFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> items = new ArrayList<>();
		try(final BufferedReader br = new BufferedReader(new FileReader(ITEM_OUTPUT_FILE))) {
			final CSVParser csvParser = CSVFormat.RFC4180.parse(br);
			for(final CSVRecord csvRecord : csvParser) {
				items.add(csvRecord);
			}
		}
		assertEquals(1, items.size());
		final int itemIdRadix = CONFIG.getItemConfig().getNamingConfig().getRadix();
		String itemPath, itemId;
		long itemOffset;
		long itemSize;
		String modLayerAndMask;
		final CSVRecord itemRec = items.get(0);
		itemPath = itemRec.get(0);
		itemId = itemPath.substring(itemPath.lastIndexOf('/') + 1);
		itemOffset = Long.parseLong(itemRec.get(1), 0x10);
		assertEquals(Long.parseLong(itemId, itemIdRadix), itemOffset);
		itemSize = Long.parseLong(itemRec.get(2));
		assertEquals(ITEM_DATA_SIZE.get(), itemSize);
		modLayerAndMask = itemRec.get(3);
		assertEquals("0/0", modLayerAndMask);
	}
}
