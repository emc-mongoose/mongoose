package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 13.06.17.
 */
public class CreateUsing100MBPartsTest
extends EnvConfiguredScenarioTestBase {

	private static String STD_OUTPUT;
	private static long EXPECTED_COUNT;

	private static final SizeInBytes SIZE_LIMIT = new SizeInBytes("100GB");
	private static final SizeInBytes PART_SIZE = new SizeInBytes("100MB");

	static {
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("atmos", "fs"));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, Arrays.asList(100, 1000));
		EXCLUDE_PARAMS.put(
			KEY_ENV_ITEM_DATA_SIZE,
			Arrays.asList(
				new SizeInBytes(0), new SizeInBytes("10KB"), new SizeInBytes("1MB"),
				new SizeInBytes("100MB")
			)
		);
		STEP_NAME = CreateUsing100MBPartsTest.class.getSimpleName();
		SCENARIO_PATH = Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "CreateUsing100MBParts.json"
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
		ITEM_DATA_SIZE = new SizeInBytes(
			SizeInBytes.toFixedSize("128MB"), SizeInBytes.toFixedSize("16GB"), 2
		);
		EXPECTED_COUNT = SIZE_LIMIT.get() / ITEM_DATA_SIZE.getAvg();
		SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
		STD_OUT_STREAM.startRecording();
		SCENARIO.run();
		LoadJobLogFileManager.flushAll();
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
			ITEM_DATA_SIZE, EXPECTED_COUNT, 0
		);
	}

	@Test
	public void testMetricsStdout()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.CREATE, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
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
}
