package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.tests.system.util.HttpStorageMockUtil;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.model.storage.StorageDriver.BUFF_SIZE_MIN;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.Frequency;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
	
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		if(ITEM_DATA_SIZE.get() == 0) {
			return;
		}
		JOB_NAME = CreateLimitBySizeTest.class.getSimpleName();
		ThreadContext.put(KEY_STEP_NAME, JOB_NAME);
		if(ITEM_DATA_SIZE.get() > SizeInBytes.toFixedSize("1GB")) {
			SIZE_LIMIT = new SizeInBytes("100GB");
		} else if(ITEM_DATA_SIZE.get() > SizeInBytes.toFixedSize("1MB")) {
			SIZE_LIMIT = new SizeInBytes("1GB");
		} else if(ITEM_DATA_SIZE.get() > SizeInBytes.toFixedSize("10KB")){
			SIZE_LIMIT = new SizeInBytes(100_000 * ITEM_DATA_SIZE.get());
		} else {
			SIZE_LIMIT = new SizeInBytes(1_000_000 * ITEM_DATA_SIZE.get());
		}
		EXPECTED_COUNT = SIZE_LIMIT.get() / ITEM_DATA_SIZE.get();
		try {
			Files.delete(Paths.get(ITEM_OUTPUT_FILE));
		} catch(final Exception ignored) {
		}
		CONFIG_ARGS.add("--item-output-file=" + ITEM_OUTPUT_FILE);
		CONFIG_ARGS.add("--test-step-limit-size=" + SIZE_LIMIT.toString());
		EnvConfiguredScenarioTestBase.setUpClass();
		switch(STORAGE_DRIVER_TYPE) {
			case STORAGE_TYPE_FS:
				ITEM_OUTPUT_PATH = Paths.get(
					Paths.get(PathUtil.getBaseDir()).getParent().toString(), JOB_NAME
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
					STD_OUTPUT = STD_OUT_STREAM.stopRecording();
				} catch(final Throwable t) {
					LogUtil.exception(Level.ERROR, t, "Failed to run the scenario");
				}
			}
		);
		runner.start();
		TimeUnit.SECONDS.timedJoin(runner, 1000);
		FINISHED_IN_TIME = !runner.isAlive();
		runner.interrupt();
		LoadJobLogFileManager.flush(JOB_NAME);
		TimeUnit.SECONDS.sleep(10);
	}
	
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		if(ITEM_DATA_SIZE.get() == 0) {
			return;
		}
		if(STORAGE_TYPE_FS.equals(STORAGE_DRIVER_TYPE)) {
			try {
				FileUtils.deleteDirectory(new File(ITEM_OUTPUT_PATH));
			} catch(final IOException e) {
				e.printStackTrace(System.err);
			}
		}
		EnvConfiguredScenarioTestBase.tearDownClass();
	}
	
	@Test
	public void testFinishedInTime()
	throws Exception {
		if(ITEM_DATA_SIZE.get() == 0) {
			return;
		}
		assertTrue(FINISHED_IN_TIME);
	}
	
	@Test
	public void testMetricsLogFile()
	throws Exception {
		if(ITEM_DATA_SIZE.get() == 0) {
			return;
		}
		testMetricsLogRecords(
			getMetricsLogRecords(),
			IoType.CREATE, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			EXPECTED_COUNT, 0,
			CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
		);
	}
	
	@Test
	public void testTotalMetricsLogFile()
	throws Exception {
		if(ITEM_DATA_SIZE.get() == 0) {
			return;
		}
		testTotalMetricsLogRecords(
			getMetricsTotalLogRecords().get(0),
			IoType.CREATE, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			EXPECTED_COUNT, 0
		);
	}
	
	@Test
	public void testMetricsStdout()
	throws Exception {
		if(ITEM_DATA_SIZE.get() == 0) {
			return;
		}
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.CREATE, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
		);
	}
	
	@Test
	public void testIoTraceLogFile()
	throws Exception {
		if(ITEM_DATA_SIZE.get() == 0) {
			return;
		}
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertEquals(EXPECTED_COUNT, ioTraceRecords.size());
		if(STORAGE_DRIVER_TYPE.equals(STORAGE_TYPE_FS)) {
			File nextItemFile;
			for(final CSVRecord ioTraceRecord : ioTraceRecords) {
				testIoTraceRecord(ioTraceRecord, IoType.CREATE.ordinal(), ITEM_DATA_SIZE);
				nextItemFile = new File(ITEM_OUTPUT_PATH, ioTraceRecord.get("ItemPath"));
				assertTrue(nextItemFile.exists());
				assertEquals(
					Long.parseLong(ioTraceRecord.get("TransferSize")), nextItemFile.length()
				);
			}
		} else {
			final String nodeAddr = HTTP_STORAGE_MOCKS.keySet().iterator().next();
			for(final CSVRecord ioTraceRecord : ioTraceRecords) {
				testIoTraceRecord(ioTraceRecord, IoType.CREATE.ordinal(), ITEM_DATA_SIZE);
				HttpStorageMockUtil.checkItem(
					nodeAddr, ioTraceRecord.get("ItemPath"),
					Long.parseLong(ioTraceRecord.get("TransferSize"))
				);
			}
		}
	}
	
	@Test
	public void testIoBufferSizeAdjustment()
	throws Exception {
		if(ITEM_DATA_SIZE.get() == 0) {
			return;
		}
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
	
	@Test
	public void testItemsOutputFile()
	throws Exception {
		if(ITEM_DATA_SIZE.get() == 0) {
			return;
		}
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
