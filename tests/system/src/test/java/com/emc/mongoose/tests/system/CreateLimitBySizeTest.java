package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.common.SizeInBytes;
import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.tests.system.util.HttpStorageMockUtil;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;

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
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 06.06.17.
 */
public class CreateLimitBySizeTest
extends EnvConfiguredScenarioTestBase {
	
	private static boolean FINISHED_IN_TIME = true;
	private static String STD_OUTPUT = null;
	private static final String ITEM_OUTPUT_FILE = CreateLimitBySizeTest.class.getSimpleName() +
		".csv";
	private static String ITEM_OUTPUT_PATH = null;
	private static SizeInBytes SIZE_LIMIT;
	private static long EXPECTED_COUNT;

	static {
		EXCLUDE_PARAMS.clear();
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("fs", "atmos"));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, Arrays.asList(1));
		EXCLUDE_PARAMS.put(
			KEY_ENV_ITEM_DATA_SIZE,
			Arrays.asList(new SizeInBytes(0), new SizeInBytes("100MB"), new SizeInBytes("10GB"))
		);
		STEP_ID = CreateLimitBySizeTest.class.getSimpleName();
	}
	
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ThreadContext.put(KEY_TEST_STEP_ID, STEP_ID);
		EnvConfiguredScenarioTestBase.setUpClass();
		if(SKIP_FLAG) {
			return;
		}
		if(ITEM_DATA_SIZE.get() > SizeInBytes.toFixedSize("1GB")) {
			SIZE_LIMIT = new SizeInBytes(100 * ITEM_DATA_SIZE.get());
		} else if(ITEM_DATA_SIZE.get() > SizeInBytes.toFixedSize("1MB")) {
			SIZE_LIMIT = new SizeInBytes(1_000 * ITEM_DATA_SIZE.get());
		} else if(ITEM_DATA_SIZE.get() > SizeInBytes.toFixedSize("10KB")){
			SIZE_LIMIT = new SizeInBytes(10_000 * ITEM_DATA_SIZE.get());
		} else {
			SIZE_LIMIT = new SizeInBytes(100_000 * ITEM_DATA_SIZE.get());
		}
		EXPECTED_COUNT = SIZE_LIMIT.get() / ITEM_DATA_SIZE.get();
		try {
			Files.delete(Paths.get(ITEM_OUTPUT_FILE));
		} catch(final Exception ignored) {
		}
		CONFIG.getItemConfig().getOutputConfig().setFile(ITEM_OUTPUT_FILE);
		CONFIG.getTestConfig().getStepConfig().getLimitConfig().setSize(SIZE_LIMIT);
		switch(STORAGE_DRIVER_TYPE) {
			case STORAGE_TYPE_FS:
				ITEM_OUTPUT_PATH = Paths.get(
					Paths.get(PathUtil.getBaseDir()).getParent().toString(), STEP_ID
				).toString();
				CONFIG.getItemConfig().getOutputConfig().setPath(ITEM_OUTPUT_PATH);
				break;
			case STORAGE_TYPE_SWIFT:
				CONFIG.getStorageConfig().getNetConfig().getHttpConfig().setNamespace("ns1");
				break;
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
		TimeUnit.SECONDS.timedJoin(runner, 1000);
		FINISHED_IN_TIME = !runner.isAlive();
		runner.interrupt();
		LogUtil.flushAll();
		TimeUnit.SECONDS.sleep(10);
	}
	
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		if(! SKIP_FLAG) {
			if(STORAGE_TYPE_FS.equals(STORAGE_DRIVER_TYPE)) {
				try {
					DirWithManyFilesDeleter.deleteExternal(ITEM_OUTPUT_PATH);
				} catch(final IOException e) {
					e.printStackTrace(System.err);
				}
			}
		}
		EnvConfiguredScenarioTestBase.tearDownClass();
	}
	
	@Test
	public void testFinishedInTime()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		assertTrue(FINISHED_IN_TIME);
	}
	
	@Test
	public void testMetricsLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		testMetricsLogRecords(
			getMetricsLogRecords(),
			IoType.CREATE, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			EXPECTED_COUNT, 0,
			CONFIG.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
	}
	
	@Test
	public void testTotalMetricsLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		testTotalMetricsLogRecord(
			getMetricsTotalLogRecords().get(0),
			IoType.CREATE, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			EXPECTED_COUNT, 0
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
		testMetricsTableStdout(
			STD_OUTPUT, STEP_ID, STORAGE_DRIVERS_COUNT, EXPECTED_COUNT,
			new HashMap<IoType, Integer>() {{ put(IoType.CREATE, CONCURRENCY); }}
		);
	}
	
	@Test
	public void testIoTraceLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertEquals(EXPECTED_COUNT, ioTraceRecords.size());
		final String nodeAddr = HTTP_STORAGE_MOCKS.keySet().iterator().next();
		for(final CSVRecord ioTraceRecord : ioTraceRecords) {
			testIoTraceRecord(ioTraceRecord, IoType.CREATE.ordinal(), ITEM_DATA_SIZE);
			HttpStorageMockUtil.assertItemExists(
				nodeAddr, ioTraceRecord.get("ItemPath"),
				Long.parseLong(ioTraceRecord.get("TransferSize"))
			);
		}
	}
	
	@Test
	public void testIoBufferSizeAdjustment()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		String msg = "Adjust output buffer size: " + ITEM_DATA_SIZE.toString();
		int k;
		for(int i = 0; i < STORAGE_DRIVERS_COUNT; i ++) {
			k = STD_OUTPUT.indexOf(msg);
			if(k > -1) {
				msg = STD_OUTPUT.substring(k + msg.length());
			} else {
				fail("Expected the message to occur " + STORAGE_DRIVERS_COUNT + " times, but got " + i);
			}
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
		assertEquals(EXPECTED_COUNT, items.size());
		final int itemIdRadix = CONFIG.getItemConfig().getNamingConfig().getRadix();
		final Frequency freq = new Frequency();
		String itemPath, itemId;
		long itemOffset;
		long itemSize;
		String modLayerAndMask;
		for(final CSVRecord itemRec : items) {
			itemPath = itemRec.get(0);
			itemId = itemPath.substring(itemPath.lastIndexOf('/') + 1);
			itemOffset = Long.parseLong(itemRec.get(1), 0x10);
			assertEquals(Long.parseLong(itemId, itemIdRadix), itemOffset);
			freq.addValue(itemOffset);
			itemSize = Long.parseLong(itemRec.get(2));
			assertEquals(ITEM_DATA_SIZE.get(), itemSize);
			modLayerAndMask = itemRec.get(3);
			assertEquals("0/0", modLayerAndMask);
		}
		assertEquals(items.size(), freq.getUniqueCount());
	}
}
