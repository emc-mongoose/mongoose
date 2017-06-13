package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.Frequency;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 06.02.17.
 * 2.2.1. Items Input File
 * 2.3.2. Items Output File
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
extends EnvConfiguredScenarioTestBase {

	static {
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, Arrays.asList(1000));
		EXCLUDE_PARAMS.put(
			KEY_ENV_ITEM_DATA_SIZE,
			Arrays.asList(new SizeInBytes(0), new SizeInBytes("100MB"), new SizeInBytes("10GB"))
		);
		STEP_NAME = CircularAppendTest.class.getSimpleName();
		SCENARIO_PATH = Paths.get(getBaseDir(), DIR_SCENARIO, "systest", "CircularAppend.json");
	}

	private static final int EXPECTED_APPEND_COUNT = 100;
	private static final long EXPECTED_COUNT = 100;
	private static final String ITEM_OUTPUT_FILE_1 = "CircularAppendTest1.csv";

	private static String STD_OUTPUT;
	private static String ITEM_OUTPUT_PATH;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		STEP_NAME = CircularAppendTest.class.getSimpleName();
		ThreadContext.put(KEY_STEP_NAME, STEP_NAME);
		CONFIG_ARGS.add("--storage-net-http-namespace=ns1");
		EnvConfiguredScenarioTestBase.setUpClass();
		if(EXCLUDE_FLAG) {
			return;
		}
		if(STORAGE_DRIVER_TYPE.equals(STORAGE_TYPE_FS)) {
			ITEM_OUTPUT_PATH = Paths.get(
				Paths.get(PathUtil.getBaseDir()).getParent().toString(), STEP_NAME
			).toString();
			CONFIG.getItemConfig().getOutputConfig().setPath(ITEM_OUTPUT_PATH);
		}
		SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
		STD_OUT_STREAM.startRecording();
		SCENARIO.run();
		STD_OUTPUT = STD_OUT_STREAM.stopRecordingAndGet();
		LoadJobLogFileManager.flushAll();
		TimeUnit.SECONDS.sleep(10);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		if(!EXCLUDE_FLAG) {
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
	public void testMetricsLogFile()
	throws Exception {
		assumeFalse(EXCLUDE_FLAG);
		try {
			final List<CSVRecord> metricsLogRecords = getMetricsLogRecords();
			assertTrue(
				"There should be more than 0 metrics records in the log file",
				metricsLogRecords.size() > 0
			);
			testMetricsLogRecords(
				metricsLogRecords, IoType.UPDATE, CONCURRENCY, STORAGE_DRIVERS_COUNT,
				ITEM_DATA_SIZE, EXPECTED_APPEND_COUNT * EXPECTED_COUNT, 0,
				CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
			);
		} catch(final FileNotFoundException ignored) {
			// there may be no metrics file if append step duration is less than 10s
		}
	}

	@Test
	public void testTotalMetricsLogFile()
	throws Exception {
		assumeFalse(EXCLUDE_FLAG);
		final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords();
		assertEquals(
			"There should be 1 total metrics records in the log file", 1,
			totalMetrcisLogRecords.size()
		);
		testTotalMetricsLogRecord(
			totalMetrcisLogRecords.get(0), IoType.UPDATE, CONCURRENCY, STORAGE_DRIVERS_COUNT,
			ITEM_DATA_SIZE, 0, 0
		);
	}

	@Test
	public void testMetricsStdout()
	throws Exception {
		assumeFalse(EXCLUDE_FLAG);
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.UPDATE, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
		);
	}

	@Test
	public void testIoTraceLogFile()
	throws Exception {
		assumeFalse(EXCLUDE_FLAG);
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertTrue(
			"There should be more than " + EXPECTED_COUNT +
				" records in the I/O trace log file, but got: " + ioTraceRecords.size(),
			EXPECTED_COUNT < ioTraceRecords.size()
		);
		for(final CSVRecord ioTraceRecord : ioTraceRecords) {
			testIoTraceRecord(ioTraceRecord, IoType.UPDATE.ordinal(), ITEM_DATA_SIZE);
		}
	}

	@Test
	public void testUpdatedItemsOutputFile()
	throws Exception {
		assumeFalse(EXCLUDE_FLAG);
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
		final SizeInBytes expectedFinalSize = new SizeInBytes(
			ITEM_DATA_SIZE.get(), 2 * EXPECTED_APPEND_COUNT * ITEM_DATA_SIZE.get(), 1
		);
		for(final CSVRecord itemRec : items) {
			itemPath = itemRec.get(0);
			itemId = itemPath.substring(itemPath.lastIndexOf('/') + 1);
			itemOffset = Long.parseLong(itemRec.get(1), 0x10);
			assertEquals(Long.parseLong(itemId, itemIdRadix), itemOffset);
			freq.addValue(itemOffset);
			itemSize = Long.parseLong(itemRec.get(2));
			assertTrue(
				"Expected size: " + expectedFinalSize.toString() + ", actual: " + itemSize,
				expectedFinalSize.getMin() <= itemSize && itemSize <= expectedFinalSize.getMax()
			);
			assertEquals("0/0", itemRec.get(3));
		}
		assertEquals(EXPECTED_COUNT, freq.getUniqueCount());
	}
}
