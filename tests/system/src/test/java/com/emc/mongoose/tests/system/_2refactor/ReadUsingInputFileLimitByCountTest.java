package com.emc.mongoose.tests.system._2refactor;

import com.emc.mongoose.api.common.SizeInBytes;
import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.deprecated.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

/**
 Created by andrey on 07.06.17.
 */
public final class ReadUsingInputFileLimitByCountTest
extends ScenarioTestBase {

	private static long EXPECTED_COUNT = 10_000;
	private static String STD_OUTPUT = null;
	private static String ITEM_OUTPUT_PATH = null;

	@Before
	public final void setUp()
	throws Exception {
		EXCLUDE_PARAMS.clear();
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("atmos", "s3"));
		EXCLUDE_PARAMS.put(
			KEY_ENV_itemSize.getValue(),
			Arrays.asList(new SizeInBytes("1MB"), new SizeInBytes("100MB"), new SizeInBytes("10GB"))
		);
		stepId = ReadUsingInputFileLimitByCountTest.class.getSimpleName();
		scenarioPath = Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "ReadUsingInputFileLimitByCount.json"
		);ThreadContext.put(KEY_TEST_STEP_ID, stepId);
		super.setUp();
		if(SKIP_FLAG) {
			return;
		}
		switch(storageType) {
			case STORAGE_TYPE_FS:
				ITEM_OUTPUT_PATH = Paths.get(
					Paths.get(PathUtil.getBaseDir()).getParent().toString(), stepId
				).toString();
				config.getItemConfig().getOutputConfig().setPath(ITEM_OUTPUT_PATH);
				break;
			case STORAGE_TYPE_SWIFT:
				config.getStorageConfig().getNetConfig().getHttpConfig().setNamespace("ns1");
				break;
		}
		try {
			scenario = new JsonScenario(config, scenarioPath.toFile());
			stdOutStream.startRecording();
			scenario.run();
			STD_OUTPUT = stdOutStream.stopRecordingAndGet();
		} catch(final Throwable t) {
			LogUtil.exception(Level.ERROR, t, "Failed to run the scenario");
		}
		LogUtil.flushAll();
		TimeUnit.SECONDS.sleep(10);
	}

	@After
	public final void tearDown()
	throws Exception {
		if(! SKIP_FLAG) {
			if(StorageType.FS.equals(storageType)) {
				try {
					FileUtils.deleteDirectory(new File(ITEM_OUTPUT_PATH));
				} catch(final IOException e) {
					e.printStackTrace(System.err);
				}
			}
		}
		super.tearDown();
	}

	@Test
	public void testMetricsLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		testMetricsLogRecords(
			getMetricsLogRecords(),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			EXPECTED_COUNT, 0, config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
	}

	@Test
	public void testTotalMetricsLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		testTotalMetricsLogRecord(
			getMetricsTotalLogRecords().get(0),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(), EXPECTED_COUNT, 0
		);
	}

	@Test
	public void testMetricsStdout()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
	}

	@Test
	public void testIoTraceLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertEquals(EXPECTED_COUNT, ioTraceRecords.size());
		for(final CSVRecord ioTraceRecord : ioTraceRecords) {
			testIoTraceRecord(ioTraceRecord, IoType.READ.ordinal(), itemSize.getValue());
		}
	}
}
