package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import static com.emc.mongoose.common.Constants.KEY_STEP_ID;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import com.emc.mongoose.tests.system.util.EnvUtil;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 13.06.17.
 */
public class MultiPartCreateTest
extends EnvConfiguredScenarioTestBase {

	private static String STD_OUTPUT;
	private static long EXPECTED_COUNT;
	private static SizeInBytes PART_SIZE;
	private static SizeInBytes SIZE_LIMIT;
	
	private static final String ITEM_OUTPUT_FILE = MultiPartCreateTest.class.getSimpleName() +
		"Items.csv";

	static {
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("atmos", "fs"));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, Arrays.asList(1000));
		EXCLUDE_PARAMS.put(
			KEY_ENV_ITEM_DATA_SIZE, Arrays.asList(
				new SizeInBytes(0), new SizeInBytes("10KB"), new SizeInBytes("10GB")
			)
		);
		STEP_NAME = MultiPartCreateTest.class.getSimpleName();
		SCENARIO_PATH = Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "MultiPartCreate.json"
		);
	}

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ThreadContext.put(KEY_STEP_ID, STEP_NAME);
		EnvConfiguredScenarioTestBase.setUpClass();
		if(SKIP_FLAG) {
			return;
		}
		PART_SIZE = ITEM_DATA_SIZE;
		ITEM_DATA_SIZE = new SizeInBytes(PART_SIZE.get(), 100 * PART_SIZE.get(), 3);
		CONFIG.getItemConfig().getDataConfig().setSize(ITEM_DATA_SIZE);
		EnvUtil.set("PART_SIZE", PART_SIZE.toString());
		EnvUtil.set("ITEM_OUTPUT_FILE", ITEM_OUTPUT_FILE);
		SIZE_LIMIT = new SizeInBytes(
			Math.min(SizeInBytes.toFixedSize("100GB"), 1000 * CONCURRENCY * PART_SIZE.get())
		);
		EnvUtil.set("SIZE_LIMIT", SIZE_LIMIT.toString());
		EXPECTED_COUNT = SIZE_LIMIT.get() / ITEM_DATA_SIZE.getAvg();
		SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
		STD_OUT_STREAM.startRecording();
		SCENARIO.run();
		LogUtil.flushAll();
		STD_OUTPUT = STD_OUT_STREAM.stopRecordingAndGet();
		TimeUnit.SECONDS.sleep(5);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		EnvConfiguredScenarioTestBase.tearDownClass();
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
			totalMetrcisLogRecords.get(0), IoType.CREATE, CONCURRENCY, STORAGE_DRIVERS_COUNT,
			ITEM_DATA_SIZE, 0, 0
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
			"There should be more than " + EXPECTED_COUNT + " records in the I/O trace log file, " +
				"but got: " + ioTraceRecords.size(),
			EXPECTED_COUNT < ioTraceRecords.size()
		);
		final SizeInBytes ZERO_SIZE = new SizeInBytes(0);
		final SizeInBytes TAIL_PART_SIZE = new SizeInBytes(1, PART_SIZE.get(), 1);
		for(final CSVRecord ioTraceRecord : ioTraceRecords) {
			try {
				testIoTraceRecord(ioTraceRecord, IoType.CREATE.ordinal(), ZERO_SIZE);
			} catch(final AssertionError e) {
				try {
					testIoTraceRecord(ioTraceRecord, IoType.CREATE.ordinal(), PART_SIZE);
				} catch(final AssertionError ee) {
					testIoTraceRecord(ioTraceRecord, IoType.CREATE.ordinal(), TAIL_PART_SIZE);
				}
			}
		}
	}

	@Test
	public void testItemsOutputFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> itemRecs = new ArrayList<>();
		try(final BufferedReader br = new BufferedReader(new FileReader(ITEM_OUTPUT_FILE))) {
			try(final CSVParser csvParser = CSVFormat.RFC4180.parse(br)) {
				for(final CSVRecord csvRecord : csvParser) {
					itemRecs.add(csvRecord);
				}
			}
		}
		long nextItemSize;
		long sizeSum = 0;
		final int n = itemRecs.size();
		assertEquals(EXPECTED_COUNT, n, EXPECTED_COUNT / 2);
		for(int i = 0; i < n; i ++) {
			nextItemSize = Long.parseLong(itemRecs.get(i).get(2));
			assertTrue(ITEM_DATA_SIZE.getMin() <= nextItemSize);
			assertTrue(ITEM_DATA_SIZE.getMax() >= nextItemSize);
			sizeSum += nextItemSize;
		}
		final long expectedAvgSize = ITEM_DATA_SIZE.getAvg();
		assertEquals(expectedAvgSize, sizeSum / n, expectedAvgSize / 5);
	}
}
