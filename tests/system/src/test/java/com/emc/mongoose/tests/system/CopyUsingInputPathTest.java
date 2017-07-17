package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.common.SizeInBytes;
import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.tests.system.util.EnvUtil;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import com.emc.mongoose.tests.system.util.HttpStorageMockUtil;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.commons.csv.CSVRecord;

import org.apache.logging.log4j.ThreadContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 Created by andrey on 12.06.17.
 */
public class CopyUsingInputPathTest
extends EnvConfiguredScenarioTestBase {

	static {
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("atmos"));
		EXCLUDE_PARAMS.put(
			KEY_ENV_ITEM_DATA_SIZE,
			Arrays.asList(new SizeInBytes("100MB"), new SizeInBytes("10GB"))
		);
		STEP_NAME = CopyUsingInputPathTest.class.getSimpleName();
		SCENARIO_PATH = Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "CopyUsingInputPath.json"
		);
	}

	private static String ITEM_SRC_PATH;
	private static String ITEM_DST_PATH;
	private static String STD_OUTPUT;

	private static final int COUNT_LIMIT = 1_000_000;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ThreadContext.put(KEY_TEST_STEP_ID, STEP_NAME);
		EnvConfiguredScenarioTestBase.setUpClass();
		if(SKIP_FLAG) {
			return;
		}
		if(STORAGE_DRIVER_TYPE.equals(STORAGE_TYPE_FS)) {
			ITEM_SRC_PATH = Paths.get(
				Paths.get(PathUtil.getBaseDir()).getParent().toString(), STEP_NAME
			).toString();
		} else {
			ITEM_SRC_PATH = '/' + STEP_NAME;
		}
		ITEM_DST_PATH = ITEM_SRC_PATH + "Dst";
		ITEM_SRC_PATH += "Src";
		EnvUtil.set("ITEM_SRC_PATH", ITEM_SRC_PATH);
		EnvUtil.set("ITEM_DST_PATH", ITEM_DST_PATH);
		SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
		STD_OUT_STREAM.startRecording();
		SCENARIO.run();
		LogUtil.flushAll();
		STD_OUTPUT = STD_OUT_STREAM.stopRecordingAndGet();
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		if(! SKIP_FLAG) {
			if(STORAGE_DRIVER_TYPE.equals(STORAGE_TYPE_FS)) {
				try {
					DirWithManyFilesDeleter.deleteExternal(ITEM_SRC_PATH);
				} catch(final Exception e) {
					e.printStackTrace(System.err);
				}
				try {
					DirWithManyFilesDeleter.deleteExternal(ITEM_DST_PATH);
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
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> metricsLogRecords = getMetricsLogRecords();
		assertTrue(
			"There should be more than 0 metrics records in the log file",
			metricsLogRecords.size() > 0
		);
		if(STORAGE_DRIVER_TYPE.equals(STORAGE_TYPE_FS)) {
			// some files may remain not written fully
			testMetricsLogRecords(metricsLogRecords, IoType.CREATE, CONCURRENCY,
				STORAGE_DRIVERS_COUNT,
				new SizeInBytes(ITEM_DATA_SIZE.get() / 2, ITEM_DATA_SIZE.get(), 1),
				0, 0, CONFIG.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
			);
		} else {
			testMetricsLogRecords(metricsLogRecords, IoType.CREATE, CONCURRENCY,
				STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE, 0, 0,
				CONFIG.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
			);
		}
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
		if(STORAGE_DRIVER_TYPE.equals(STORAGE_TYPE_FS)) {
			// some files may remain not written fully
			testTotalMetricsLogRecord(
				totalMetrcisLogRecords.get(0), IoType.CREATE, CONCURRENCY, STORAGE_DRIVERS_COUNT,
				new SizeInBytes(ITEM_DATA_SIZE.get() / 2, ITEM_DATA_SIZE.get(), 1), 0, 0
			);
		} else {
			testTotalMetricsLogRecord(
				totalMetrcisLogRecords.get(0), IoType.CREATE, CONCURRENCY, STORAGE_DRIVERS_COUNT,
				ITEM_DATA_SIZE, 0, 0
			);
		}
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
			"There should be " + COUNT_LIMIT + " records in the I/O trace log file",
			ioTraceRecords.size() <= COUNT_LIMIT
		);
		String nextItemPath, nextItemId;
		if(STORAGE_DRIVER_TYPE.equals(STORAGE_TYPE_FS)) {
			File nextSrcFile;
			File nextDstFile;
			for(final CSVRecord ioTraceRecord : ioTraceRecords) {
				nextItemPath = ioTraceRecord.get("ItemPath");
				nextSrcFile = new File(nextItemPath);
				nextItemId = nextItemPath.substring(
					nextItemPath.lastIndexOf(File.separatorChar) + 1
				);
				nextDstFile = Paths.get(ITEM_SRC_PATH, nextItemId).toFile();
				Assert.assertTrue(
					"File \"" + nextItemPath + "\" doesn't exist", nextSrcFile.exists()
				);
				Assert.assertTrue(
					"File \"" + nextDstFile.getPath() + "\" doesn't exist", nextDstFile.exists()
				);
				Assert.assertEquals(nextSrcFile.length(), nextDstFile.length());
				testIoTraceRecord(
					ioTraceRecord, IoType.CREATE.ordinal(), new SizeInBytes(nextSrcFile.length())
				);
			}
		} else {
			final String node = HTTP_STORAGE_MOCKS.keySet().iterator().next();
			for(final CSVRecord ioTraceRecord : ioTraceRecords) {
				testIoTraceRecord(ioTraceRecord, IoType.CREATE.ordinal(), ITEM_DATA_SIZE);
				nextItemPath = ioTraceRecord.get("ItemPath");
				//HttpStorageMockUtil.assertItemExists(node, nextItemPath, ITEM_DATA_SIZE.get());
				nextItemId = nextItemPath.substring(nextItemPath.lastIndexOf(File.separatorChar) + 1);
				HttpStorageMockUtil.assertItemExists(
					node, ITEM_SRC_PATH + '/' + nextItemId, ITEM_DATA_SIZE.get()
				);
			}
		}
	}
}
