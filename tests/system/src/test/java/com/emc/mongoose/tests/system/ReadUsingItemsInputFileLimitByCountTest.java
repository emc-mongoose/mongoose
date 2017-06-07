package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.run.scenario.Scenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 07.06.17.
 */
public class ReadUsingItemsInputFileLimitByCountTest
extends EnvConfiguredScenarioTestBase {

	private static final int EXPECTED_COUNT = 1000;
	private static String STD_OUTPUT = null;
	private static String ITEM_OUTPUT_PATH = null;

	static {
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("atmos", "s3"));
		EXCLUDE_PARAMS.put(
			KEY_ENV_ITEM_DATA_SIZE, Arrays.asList(new SizeInBytes("100MB"), new SizeInBytes("10GB"))
		);
		STEP_NAME = ReadUsingItemsInputFileLimitByCountTest.class.getSimpleName();
		SCENARIO_PATH = Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "ReadUsingItemsInputFileLimitByCount.json"
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
		switch(STORAGE_DRIVER_TYPE) {
			case STORAGE_TYPE_FS:
				ITEM_OUTPUT_PATH = Paths.get(
					Paths.get(PathUtil.getBaseDir()).getParent().toString(), STEP_NAME
				).toString();
				CONFIG.getItemConfig().getOutputConfig().setPath(ITEM_OUTPUT_PATH);
				break;
			case STORAGE_TYPE_SWIFT:
				CONFIG.getStorageConfig().getNetConfig().getHttpConfig().setNamespace("ns1");
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
		if(!EXCLUDE_FLAG) {
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
		if(EXCLUDE_FLAG) {
			return;
		}
		testMetricsLogRecords(
			getMetricsLogRecords(),
			IoType.READ, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			EXPECTED_COUNT, 0,
			CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
		);
	}

	@Test
	public void testTotalMetricsLogFile()
	throws Exception {
		if(EXCLUDE_FLAG) {
			return;
		}
		testTotalMetricsLogRecords(
			getMetricsTotalLogRecords().get(0),
			IoType.READ, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			EXPECTED_COUNT, 0
		);
	}

	@Test
	public void testMetricsStdout()
	throws Exception {
		if(EXCLUDE_FLAG) {
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
		if(EXCLUDE_FLAG) {
			return;
		}
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertEquals(EXPECTED_COUNT, ioTraceRecords.size());
		for(final CSVRecord ioTraceRecord : ioTraceRecords) {
			testIoTraceRecord(ioTraceRecord, IoType.READ.ordinal(), ITEM_DATA_SIZE);
		}
	}
}
