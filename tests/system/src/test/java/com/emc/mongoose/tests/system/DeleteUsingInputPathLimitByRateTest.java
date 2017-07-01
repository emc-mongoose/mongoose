package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.tests.system.util.EnvUtil;
import com.emc.mongoose.tests.system.util.HttpStorageMockUtil;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.common.Constants.KEY_TEST_STEP_ID;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 07.06.17.
 */
public class DeleteUsingInputPathLimitByRateTest
extends EnvConfiguredScenarioTestBase {

	private static final int EXPECTED_COUNT = 100_000;
	private static final int EXPECTED_RATE = 1234;
	private static String STD_OUTPUT = null;
	private static String ITEM_OUTPUT_PATH = null;

	static {
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("atmos"));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, Arrays.asList(1));
		EXCLUDE_PARAMS.put(
			KEY_ENV_ITEM_DATA_SIZE,
			Arrays.asList(new SizeInBytes("100MB"), new SizeInBytes("10GB"))
		);
		STEP_NAME = DeleteUsingInputPathLimitByRateTest.class.getSimpleName();
		SCENARIO_PATH = Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "DeleteUsingInputPathLimitByRate.json"
		);
	}

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ThreadContext.put(KEY_TEST_STEP_ID, STEP_NAME);
		EnvConfiguredScenarioTestBase.setUpClass();
		if(SKIP_FLAG) {
			return;
		}
		switch(STORAGE_DRIVER_TYPE) {
			case STORAGE_TYPE_FS:
				ITEM_OUTPUT_PATH = Paths.get(
					Paths.get(PathUtil.getBaseDir()).getParent().toString(), STEP_NAME
				).toString();
				EnvUtil.set("ITEMS_PATH", ITEM_OUTPUT_PATH);
				break;
			case STORAGE_TYPE_SWIFT:
				CONFIG.getStorageConfig().getNetConfig().getHttpConfig().setNamespace("ns1");
			default:
				EnvUtil.set("ITEMS_PATH", "/" + STEP_NAME);
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
		LogUtil.flushAll();
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
		final List<CSVRecord> metricsLogRecs = getMetricsLogRecords();
		testMetricsLogRecords(
			metricsLogRecs, IoType.DELETE, CONCURRENCY, STORAGE_DRIVERS_COUNT, new SizeInBytes(0),
			EXPECTED_COUNT, 0, CONFIG.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
		for(final CSVRecord metricsLogRec : metricsLogRecs) {
			assertEquals(
				EXPECTED_RATE, Double.parseDouble(metricsLogRec.get("TPAvg[op/s]")),
				EXPECTED_RATE
			);
		}
	}

	@Test
	public void testTotalMetricsLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final CSVRecord totalRec = getMetricsTotalLogRecords().get(0);
		testTotalMetricsLogRecord(
			totalRec,
			IoType.DELETE, CONCURRENCY, STORAGE_DRIVERS_COUNT, new SizeInBytes(0), EXPECTED_COUNT, 0
		);
		assertEquals(
			EXPECTED_RATE, Double.parseDouble(totalRec.get("TPAvg[op/s]")), EXPECTED_RATE / 10
		);
	}

	@Test
	public void testMetricsStdout()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.DELETE, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			CONFIG.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
		testMetricsTableStdout(
			STD_OUTPUT, STEP_NAME, STORAGE_DRIVERS_COUNT, EXPECTED_COUNT,
			new HashMap<IoType, Integer>() {{ put(IoType.DELETE, CONCURRENCY); }}
		);
	}

	@Test
	public void testIoTraceLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertTrue(ioTraceRecords.size() > 0);
		String nextItemPath;
		if(STORAGE_DRIVER_TYPE.equals(STORAGE_TYPE_FS)) {
			for(final CSVRecord ioTraceRecord : ioTraceRecords) {
				testIoTraceRecord(ioTraceRecord, IoType.DELETE.ordinal(), new SizeInBytes(0));
				nextItemPath = ioTraceRecord.get("ItemPath");
				assertFalse(Files.exists(Paths.get(nextItemPath)));
			}
		} else {
			final String nodeAddr = CONFIG.getStorageConfig().getNetConfig().getNodeConfig().getAddrs().get(0);
			for(final CSVRecord ioTraceRecord : ioTraceRecords) {
				testIoTraceRecord(ioTraceRecord, IoType.DELETE.ordinal(), new SizeInBytes(0));
				nextItemPath = ioTraceRecord.get("ItemPath");
				HttpStorageMockUtil.assertItemNotExists(nodeAddr, nextItemPath);
			}
		}
	}
}
