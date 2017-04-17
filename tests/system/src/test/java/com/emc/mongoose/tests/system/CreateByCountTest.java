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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.model.storage.StorageDriver.BUFF_SIZE_MIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 Created by andrey on 19.01.17.
 Covered use cases:
 * 2.1.1.1.2. Small Data Items (1B-100KB)
 * 2.2.3.1. Random Item Ids
 * 2.3.2. Items Output File
 * 4.2. Small Concurrency Level (2-10)
 * 6.1. Test Step Naming
 * 6.2.2. Limit Step by Processed Item Count
 * 7.1. Metrics Periodic Reporting
 * 7.4. I/O Traces Reporting
 * 8.2.1. Create New Items
 * 9.2. Default Scenario
 * 10.1.4. Many Remote Storage Driver Services
 * 10.4.4. I/O Buffer Size Adjustment for Optimal Performance
 */
public class CreateByCountTest
extends HttpStorageDistributedScenarioTestBase {
	
	private static final SizeInBytes ITEM_DATA_SIZE = new SizeInBytes("10B");
	private static final String ITEM_OUTPUT_FILE = CreateByCountTest.class.getSimpleName() + ".csv";
	private static final int LOAD_LIMIT_COUNT = 100;
	private static final int LOAD_CONCURRENCY = 10;
	
	private static boolean FINISHED_IN_TIME = true;
	private static String STD_OUTPUT = null;
	
	@BeforeClass public static void setUpClass()
	throws Exception {
		JOB_NAME = CreateByCountTest.class.getSimpleName();
		ThreadContext.put(KEY_STEP_NAME, JOB_NAME);
		CONFIG_ARGS.add("--item-data-size=" + ITEM_DATA_SIZE.toString());
		CONFIG_ARGS.add("--item-output-file=" + ITEM_OUTPUT_FILE);
		CONFIG_ARGS.add("--test-step-limit-count=" + LOAD_LIMIT_COUNT);
		CONFIG_ARGS.add("--storage-driver-concurrency=" + LOAD_CONCURRENCY);
		try {
			Files.delete(Paths.get(ITEM_OUTPUT_FILE));
		} catch(final IOException ignored) {
		}
		HttpStorageDistributedScenarioTestBase.setUpClass();
		final Thread runner = new Thread(
			() -> {
				try {
					SCENARIO.run();
				} catch(final Throwable t) {
					LogUtil.exception(LOG, Level.ERROR, t, "Failed to run the scenario");
				}
			}
		);
		STD_OUT_STREAM.startRecording();
		runner.start();
		TimeUnit.SECONDS.timedJoin(runner, 1000);
		FINISHED_IN_TIME = !runner.isAlive();
		runner.interrupt();
		LoadJobLogFileManager.flush(JOB_NAME);
		TimeUnit.SECONDS.sleep(10);
		STD_OUTPUT = STD_OUT_STREAM.stopRecording();
	}
	
	@AfterClass public static void tearDownClass()
	throws Exception {
		HttpStorageDistributedScenarioTestBase.tearDownClass();
	}
	
	@Test public void testFinishedInTime()
	throws Exception {
		assertTrue(FINISHED_IN_TIME);
	}
	
	@Test @Ignore
	public void testMetricsLogFile()
	throws Exception {
		testMetricsLogRecords(
			getMetricsLogRecords(),
			IoType.CREATE, LOAD_CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE, LOAD_LIMIT_COUNT,
			0, CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
		);
	}
	
	@Test @Ignore
	public void testTotalMetricsLogFile()
	throws Exception {
		testTotalMetricsLogRecords(
			getMetricsTotalLogRecords().get(0),
			IoType.CREATE, LOAD_CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE, LOAD_LIMIT_COUNT,
			0
		);
	}
	
	@Test public void testMetricsStdout()
	throws Exception {
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.CREATE, LOAD_CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
		);
	}
	
	@Test public void testIoTraceLogFile()
	throws Exception {
		final String nodeAddr = STORAGE_MOCKS.keySet().iterator().next();
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertEquals(LOAD_LIMIT_COUNT, ioTraceRecords.size());
		for(final CSVRecord ioTraceRecord : ioTraceRecords) {
			testIoTraceRecord(ioTraceRecord, IoType.CREATE.ordinal(), ITEM_DATA_SIZE);
			testHttpStorageMockContains(
				nodeAddr, ioTraceRecord.get("ItemPath"),
				Long.parseLong(ioTraceRecord.get("TransferSize"))
			);
		}
	}
	
	@Test public void testIoBufferSizeAdjustment()
	throws Exception {
		String msg = "Adjust output buffer size: " + SizeInBytes.formatFixedSize(BUFF_SIZE_MIN);
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
	
	@Test public void testItemsOutputFile()
	throws Exception {
		final List<CSVRecord> items = new ArrayList<>();
		try(final BufferedReader br = new BufferedReader(new FileReader(ITEM_OUTPUT_FILE))) {
			final CSVParser csvParser = CSVFormat.RFC4180.parse(br);
			for(final CSVRecord csvRecord : csvParser) {
				items.add(csvRecord);
			}
		}
		assertEquals(LOAD_LIMIT_COUNT, items.size());
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
