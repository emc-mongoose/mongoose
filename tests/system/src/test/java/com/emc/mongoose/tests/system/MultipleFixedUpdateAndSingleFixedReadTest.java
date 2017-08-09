package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.common.SizeInBytes;
import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import com.emc.mongoose.ui.log.LogUtil;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.ThreadContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;

/**
 Created by kurila on 15.06.17.
 */
public class MultipleFixedUpdateAndSingleFixedReadTest
extends EnvConfiguredScenarioTestBase {
	
	private static String ITEM_OUTPUT_PATH;
	private static String STD_OUTPUT;
	private static SizeInBytes EXPECTED_UPDATE_SIZE;
	private static SizeInBytes EXPECTED_READ_SIZE;
	
	private static final long EXPECTED_COUNT = 10000;
	
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		EXCLUDE_PARAMS.clear();
		/**
		 https://github.com/emc-mongoose/nagaina/issues/3
		 */
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("atmos"));
		//EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, Arrays.asList(1));
		EXCLUDE_PARAMS.put(
			KEY_ENV_ITEM_DATA_SIZE,
			Arrays.asList(new SizeInBytes(0), new SizeInBytes("100MB"), new SizeInBytes("10GB"))
		);
		STEP_ID = MultipleFixedUpdateAndSingleFixedReadTest.class.getSimpleName();
		SCENARIO_PATH = Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "MultipleFixedUpdateAndSingleFixedRead.json"
		);
		ThreadContext.put(KEY_TEST_STEP_ID, STEP_ID);
		EnvConfiguredScenarioTestBase.setUpClass();
		if(SKIP_FLAG) {
			return;
		}
		EXPECTED_UPDATE_SIZE = new SizeInBytes(
			-LongStream.of(2-5,10-20,50-100,200-500,1000-2000).sum()
		);
		EXPECTED_READ_SIZE = new SizeInBytes(ITEM_DATA_SIZE.get() - 256);
		if(STORAGE_DRIVER_TYPE.equals(STORAGE_TYPE_FS)) {
			ITEM_OUTPUT_PATH = Paths
				.get(Paths.get(PathUtil.getBaseDir()).getParent().toString(), STEP_ID)
				.toString();
			CONFIG.getItemConfig().getOutputConfig().setPath(ITEM_OUTPUT_PATH);
		}
		SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
		STD_OUT_STREAM.startRecording();
		SCENARIO.run();
		LogUtil.flushAll();
		STD_OUTPUT = STD_OUT_STREAM.stopRecordingAndGet();
	}
	
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		if(!SKIP_FLAG) {
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
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> metricsLogRecords = getMetricsLogRecords();
		final List<CSVRecord> updateMetricsRecords = new ArrayList<>();
		final List<CSVRecord> readMetricsRecords = new ArrayList<>();
		for(final CSVRecord metricsLogRec : metricsLogRecords) {
			if(IoType.UPDATE.name().equalsIgnoreCase(metricsLogRec.get("TypeLoad"))) {
				updateMetricsRecords.add(metricsLogRec);
			} else {
				readMetricsRecords.add(metricsLogRec);
			}
		}
		testMetricsLogRecords(
			updateMetricsRecords, IoType.UPDATE, CONCURRENCY, STORAGE_DRIVERS_COUNT,
			EXPECTED_UPDATE_SIZE, EXPECTED_COUNT, 0,
			CONFIG.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
		testMetricsLogRecords(
			readMetricsRecords, IoType.READ, CONCURRENCY, STORAGE_DRIVERS_COUNT,
			EXPECTED_READ_SIZE, EXPECTED_COUNT, 0,
			CONFIG.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
	}
	
	@Test
	public void testTotalMetricsLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords();
		testTotalMetricsLogRecord(
			totalMetrcisLogRecords.get(0), IoType.UPDATE, CONCURRENCY, STORAGE_DRIVERS_COUNT,
			EXPECTED_UPDATE_SIZE, EXPECTED_COUNT, 0
		);
		testTotalMetricsLogRecord(
			totalMetrcisLogRecords.get(1), IoType.READ, CONCURRENCY, STORAGE_DRIVERS_COUNT,
			EXPECTED_READ_SIZE, EXPECTED_COUNT, 0
		);
	}
	
	@Test
	public void testMetricsStdout()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final String stdOutput = STD_OUTPUT.replaceAll("[\r\n]+", " ");
		testSingleMetricsStdout(
			stdOutput, IoType.UPDATE, CONCURRENCY, STORAGE_DRIVERS_COUNT, EXPECTED_UPDATE_SIZE,
			CONFIG.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
		testSingleMetricsStdout(
			stdOutput, IoType.READ, CONCURRENCY, STORAGE_DRIVERS_COUNT, EXPECTED_READ_SIZE,
			CONFIG.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
	}
	
	@Test
	public void testIoTraceLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertEquals(
			"There should be " + 2 * EXPECTED_COUNT + " records in the I/O trace log file",
			2 * EXPECTED_COUNT, ioTraceRecords.size()
		);
		for(int i = 0; i < 2 * EXPECTED_COUNT; i ++) {
			if(i < EXPECTED_COUNT) {
				testIoTraceRecord(ioTraceRecords.get(i), IoType.UPDATE.ordinal(), EXPECTED_UPDATE_SIZE);
			} else {
				testIoTraceRecord(ioTraceRecords.get(i), IoType.READ.ordinal(), EXPECTED_READ_SIZE);
			}
		}
	}
}
