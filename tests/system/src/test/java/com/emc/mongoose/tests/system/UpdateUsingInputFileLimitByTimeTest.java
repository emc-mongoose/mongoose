package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

/**
 Created by andrey on 07.06.17.
 */
public class UpdateUsingInputFileLimitByTimeTest
extends EnvConfiguredScenarioTestBase {

	private static final int EXPECTED_TIME = 25;
	private static String STD_OUTPUT = null;
	private static String ITEM_OUTPUT_PATH = null;

	static {
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("atmos", "swift"));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, Arrays.asList(1));
		EXCLUDE_PARAMS.put(
			KEY_ENV_ITEM_DATA_SIZE,
			Arrays.asList(
				new SizeInBytes(0), new SizeInBytes("10KB"), new SizeInBytes("100MB"),
				new SizeInBytes("10GB")
			)
		);
		STEP_NAME = UpdateUsingInputFileLimitByTimeTest.class.getSimpleName();
		SCENARIO_PATH = Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "UpdateUsingInputFileLimitByTime.json"
		);
	}

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ThreadContext.put(KEY_STEP_NAME, STEP_NAME);
		EnvConfiguredScenarioTestBase.setUpClass();
		if(SKIP_FLAG) {
			return;
		}
		switch(STORAGE_DRIVER_TYPE) {
			case STORAGE_TYPE_FS:
				ITEM_OUTPUT_PATH = Paths.get(
					Paths.get(PathUtil.getBaseDir()).getParent().toString(), STEP_NAME
				).toString();
				CONFIG.getItemConfig().getOutputConfig().setPath(ITEM_OUTPUT_PATH);
				break;
		}
		try {
			SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
			STD_OUT_STREAM.startRecording();
			SCENARIO.run();
			STD_OUTPUT = STD_OUT_STREAM.stopRecordingAndGet();
		} catch(final Throwable t) {
			LogUtil.exception(Level.ERROR, t, "Failed to run the scenario");
		}
		LoadJobLogFileManager.flush(STEP_NAME);
		TimeUnit.SECONDS.sleep(10);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		if(! SKIP_FLAG) {
			if(STORAGE_TYPE_FS.equals(STORAGE_DRIVER_TYPE)) {
				try {
					FileUtils.deleteDirectory(new File(ITEM_OUTPUT_PATH));
				} catch(final IOException e) {
					e.printStackTrace(System.err);
				}
			}
		}
		EnvConfiguredScenarioTestBase.tearDownClass();
	}

	@Test
	public void testMetricsLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		testMetricsLogRecords(
			getMetricsLogRecords(),
			IoType.UPDATE, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE, 0, EXPECTED_TIME,
			CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
		);
	}

	@Test
	public void testTotalMetricsLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final SizeInBytes updateSize = new SizeInBytes(1, ITEM_DATA_SIZE.get() / 2 + 1, 1);
		testTotalMetricsLogRecord(
			getMetricsTotalLogRecords().get(0),
			IoType.UPDATE, CONCURRENCY, STORAGE_DRIVERS_COUNT, updateSize, 0, EXPECTED_TIME
		);
	}

	@Test
	public void testMetricsStdout()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.UPDATE, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
		);
		testMetricsTableStdout(
			STD_OUTPUT, STEP_NAME, STORAGE_DRIVERS_COUNT, 0,
			new HashMap<IoType, Integer>() {{ put(IoType.UPDATE, CONCURRENCY ); }}
		);
	}

	@Test
	public void testIoTraceLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertTrue(ioTraceRecords.size() > 0);
		final SizeInBytes updateSize = new SizeInBytes(1, ITEM_DATA_SIZE.get() / 2 + 1, 1);
		for(final CSVRecord ioTraceRecord : ioTraceRecords) {
			testIoTraceRecord(ioTraceRecord, IoType.UPDATE.ordinal(), updateSize);
		}
	}

	@Test
	public void testUpdatedItemsListFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> items = new ArrayList<>();
		try(final BufferedReader br = new BufferedReader(new FileReader("UpdateUsingInputFileLimitByTimeTest_.csv"))) {
			final CSVParser csvParser = CSVFormat.RFC4180.parse(br);
			for(final CSVRecord csvRecord : csvParser) {
				items.add(csvRecord);
			}
		}

		String itemPath;
		String itemId;
		long itemOffset;
		final int itemIdRadix = CONFIG.getItemConfig().getNamingConfig().getRadix();
		long itemSize;
		String[] modLayerAndMask;
		int layer;
		String rangesMask;
		char[] rangesMaskChars;
		BitSet mask;
		for(final CSVRecord itemRec : items) {
			itemPath = itemRec.get(0);
			itemId = itemPath.substring(itemPath.lastIndexOf('/') + 1);
			itemOffset = Long.parseLong(itemRec.get(1), 0x10);
			assertEquals(Long.parseLong(itemId, itemIdRadix), itemOffset);
			itemSize = Long.parseLong(itemRec.get(2));
			assertEquals(ITEM_DATA_SIZE.get(), itemSize);
			modLayerAndMask = itemRec.get(3).split("/");
			assertEquals("Modification record should contain 2 parts", 2, modLayerAndMask.length);
			layer = Integer.parseInt(modLayerAndMask[0], 0x10);
			assertEquals(layer, 0);
			rangesMask = modLayerAndMask[1];
			if(rangesMask.length() == 0) {
				rangesMaskChars = ("00" + rangesMask).toCharArray();
			} else if(rangesMask.length() % 2 == 1) {
				rangesMaskChars = ("0" + rangesMask).toCharArray();
			} else {
				rangesMaskChars = rangesMask.toCharArray();
			}
			mask = BitSet.valueOf(Hex.decodeHex(rangesMaskChars));
			assertTrue(
				"The modification record \"" + itemRec.get(3) + "\" is not updated",
				mask.cardinality() > 0
			);
		}
	}
}
