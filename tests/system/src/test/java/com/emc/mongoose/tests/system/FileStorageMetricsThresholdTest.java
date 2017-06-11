package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.tests.system.util.LogPatterns;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.common.env.DateUtil.FMT_DATE_ISO8601;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.ThreadContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

/**
 Created by andrey on 10.06.17.
 */
public class FileStorageMetricsThresholdTest
extends EnvConfiguredScenarioTestBase {

	private static final double LOAD_THRESHOLD = 0.8;
	private static final int RANDOM_RANGES_COUNT = 10;

	private static String ITEM_OUTPUT_PATH;
	private static String STD_OUTPUT;

	static {
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("atmos", "s3", "swift"));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, Arrays.asList(1, 1000));
		EXCLUDE_PARAMS.put(
			KEY_ENV_ITEM_DATA_SIZE,
			Arrays.asList(new SizeInBytes(0), new SizeInBytes("100MB"), new SizeInBytes("10GB"))
		);
		STEP_NAME = FileStorageMetricsThresholdTest.class.getSimpleName();
		SCENARIO_PATH = Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "FileStorageMetricsThreshold.json"
		);
	}

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ThreadContext.put(KEY_STEP_NAME, STEP_NAME);
		EnvConfiguredScenarioTestBase.setUpClass();
		if(EXCLUDE_FLAG) {
			return;
		}
		ITEM_OUTPUT_PATH = Paths.get(
			Paths.get(PathUtil.getBaseDir()).getParent().toString(), STEP_NAME
		).toString();
		CONFIG.getItemConfig().getOutputConfig().setPath(ITEM_OUTPUT_PATH);
		SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
		STD_OUT_STREAM.startRecording();
		SCENARIO.run();
		LoadJobLogFileManager.flushAll();
		STD_OUTPUT = STD_OUT_STREAM.stopRecordingAndGet();
		TimeUnit.SECONDS.sleep(10);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		if(!EXCLUDE_FLAG) {
			try {
				DirWithManyFilesDeleter.deleteExternal(ITEM_OUTPUT_PATH);
			} catch(final Exception e) {
				e.printStackTrace(System.err);
			}
		}
		EnvConfiguredScenarioTestBase.tearDownClass();
	}

	@Test
	public void testMetricsLogFile()
	throws Exception {
		final List<CSVRecord> metricsLogRecs = getMetricsLogRecords();
		final List<CSVRecord> createMetricsRecs = new ArrayList<>();
		final List<CSVRecord> readMetricsRecs = new ArrayList<>();
		final List<CSVRecord> updateMetricsRecs = new ArrayList<>();
		IoType nextMetricsRecIoType;
		for(final CSVRecord metricsRec : metricsLogRecs) {
			nextMetricsRecIoType = IoType.valueOf(metricsRec.get("TypeLoad"));
			switch(nextMetricsRecIoType) {
				case NOOP:
					fail("Unexpected I/O type: " + nextMetricsRecIoType);
					break;
				case CREATE:
					createMetricsRecs.add(metricsRec);
					break;
				case READ:
					readMetricsRecs.add(metricsRec);
					break;
				case UPDATE:
					updateMetricsRecs.add(metricsRec);
					break;
				case DELETE:
					fail("Unexpected I/O type: " + nextMetricsRecIoType);
					break;
				case LIST:
					fail("Unexpected I/O type: " + nextMetricsRecIoType);
					break;
			}
		}
		final long period = CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod();
		testMetricsLogRecords(
			createMetricsRecs, IoType.CREATE, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			0, 0, period
		);
		testMetricsLogRecords(
			readMetricsRecs, IoType.READ, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			0, 0, period
		);
		testMetricsLogRecords(
			updateMetricsRecs, IoType.UPDATE, CONCURRENCY, STORAGE_DRIVERS_COUNT,
			new SizeInBytes(2 >> RANDOM_RANGES_COUNT - 1, ITEM_DATA_SIZE.get(), 1),
			0, 0, period
		);
	}

	@Test
	public void testTotalMetricsLogFile()
	throws Exception {
		final List<CSVRecord> totalMetricsRecs = getMetricsTotalLogRecords();
		testTotalMetricsLogRecord(
			totalMetricsRecs.get(0), IoType.CREATE, CONCURRENCY, STORAGE_DRIVERS_COUNT,
			ITEM_DATA_SIZE, 0, 0
		);
		testTotalMetricsLogRecord(
			totalMetricsRecs.get(1), IoType.READ, CONCURRENCY, STORAGE_DRIVERS_COUNT,
			ITEM_DATA_SIZE, 0, 0
		);
		testTotalMetricsLogRecord(
			totalMetricsRecs.get(2), IoType.UPDATE, CONCURRENCY, STORAGE_DRIVERS_COUNT,
			new SizeInBytes(2 >> RANDOM_RANGES_COUNT - 1, ITEM_DATA_SIZE.get(), 1), 0, 0
		);
	}

	@Test
	public void testMetricsStdout()
	throws Exception {
		final long period = CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod();
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.CREATE, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE, period
		);
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.READ, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE, period
		);
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.UPDATE, CONCURRENCY, STORAGE_DRIVERS_COUNT,
			new SizeInBytes(2 >> RANDOM_RANGES_COUNT - 1, ITEM_DATA_SIZE.get(), 1), period
		);
	}

	@Test
	public void testMedTotalMetricsLogFile()
	throws Exception {
		final List<CSVRecord> totalThresholdMetricsRecs = getMetricsMedTotalLogRecords();
		testTotalMetricsLogRecord(
			totalThresholdMetricsRecs.get(0), IoType.CREATE, CONCURRENCY, STORAGE_DRIVERS_COUNT,
			ITEM_DATA_SIZE, 0, 0
		);
		testTotalMetricsLogRecord(
			totalThresholdMetricsRecs.get(1), IoType.READ, CONCURRENCY, STORAGE_DRIVERS_COUNT,
			ITEM_DATA_SIZE, 0, 0
		);
		testTotalMetricsLogRecord(
			totalThresholdMetricsRecs.get(2), IoType.UPDATE, CONCURRENCY, STORAGE_DRIVERS_COUNT,
			new SizeInBytes(2 >> RANDOM_RANGES_COUNT - 1, ITEM_DATA_SIZE.get(), 1), 0, 0
		);
	}

	@Test
	public void testThresholdConditionMessagesInStdout()
	throws Exception {
		int n = 0;
		Matcher m;
		while(true) {
			m = LogPatterns.STD_OUT_LOAD_THRESHOLD_ENTRANCE.matcher(STD_OUTPUT);
			if(!m.find()) {
				break;
			}
			final Date dtEnter = FMT_DATE_ISO8601.parse(m.group("dateTime"));
			final int threshold = Integer.parseInt(m.group("threshold"));
			assertEquals(CONCURRENCY * LOAD_THRESHOLD, threshold, 0);
			STD_OUTPUT = m.replaceFirst("");
			m = LogPatterns.STD_OUT_LOAD_THRESHOLD_EXIT.matcher(
				STD_OUTPUT.substring(m.regionStart())
			);
			assertTrue(m.find());
			final Date dtExit = FMT_DATE_ISO8601.parse(m.group("dateTime"));
			assertTrue(dtEnter.before(dtExit));
			STD_OUTPUT = m.replaceFirst("");
			n ++;
		}
		assertEquals(3, n);
	}
}
