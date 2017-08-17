package com.emc.mongoose.tests.system._2refactor;

import com.emc.mongoose.api.common.SizeInBytes;
import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.deprecated.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

/**
 Created by andrey on 12.06.17.
 */
public final class ReadVerificationDisableTest
extends ScenarioTestBase {

	private static String ITEM_OUTPUT_PATH;
	private static String STD_OUTPUT;

	@Before
	public final void setUp()
	throws Exception {
		EXCLUDE_PARAMS.clear();
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("s3", "swift"));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_concurrency.getValue(), Arrays.asList(1, 1000));
		EXCLUDE_PARAMS.put(
			KEY_ENV_itemSize.getValue(),
			Arrays.asList(new SizeInBytes(0), new SizeInBytes("100MB"), new SizeInBytes("10GB"))
		);
		stepId = ReadVerificationDisableTest.class.getSimpleName();
		scenarioPath = Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "ReadVerificationDisable.json"
		);
		ThreadContext.put(KEY_TEST_STEP_ID, stepId);
		configArgs.add("--storage-net-http-namespace=ns1");
		super.setUp();
		if(SKIP_FLAG) {
			return;
		}
		if(STORAGE_DRIVER_TYPE.equals(STORAGE_TYPE_FS)) {
			ITEM_OUTPUT_PATH = Paths.get(
				Paths.get(PathUtil.getBaseDir()).getParent().toString(), stepId
			).toString();
			config.getItemConfig().getOutputConfig().setPath(ITEM_OUTPUT_PATH);
		}
		scenario = new JsonScenario(config, scenarioPath.toFile());
		stdOutStream.startRecording();
		scenario.run();
		LogUtil.flushAll();
		STD_OUTPUT = stdOutStream.stopRecordingAndGet();
		TimeUnit.SECONDS.sleep(5);
	}

	@After
	public final void tearDown()
	throws Exception {
		if(! SKIP_FLAG) {
			if(STORAGE_DRIVER_TYPE.equals(STORAGE_TYPE_FS)) {
				try {
					DirWithManyFilesDeleter.deleteExternal(ITEM_OUTPUT_PATH);
				} catch(final Exception e) {
					e.printStackTrace(System.err);
				}
			}
		}
		super.tearDown();
	}

	@Test
	public void testIoTraceLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		CSVRecord csvRecord;
		for(int i = 0; i < ioTraceRecords.size(); i ++) {
			csvRecord = ioTraceRecords.get(i);
			assertEquals(
				"Record #" + i + ": unexpected operation type " + csvRecord.get("IoTypeCode"),
				IoType.READ, IoType.values()[Integer.parseInt(csvRecord.get("IoTypeCode"))]
			);
			assertEquals(
				"Record #" + i + ": unexpected status code " + csvRecord.get("StatusCode"),
				IoTask.Status.SUCC,
				IoTask.Status.values()[Integer.parseInt(csvRecord.get("StatusCode"))]
			);
		}
	}

	@Test
	public void testMetricsLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		testMetricsLogRecords(
			getMetricsLogRecords(),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(), 0, 0,
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
	}

	@Test
	public void testTotalMetricsLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		testTotalMetricsLogRecord(
			getMetricsTotalLogRecords().get(0),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(), 0, 0
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
		testMetricsTableStdout(
			STD_OUTPUT, stepId, driverCount.getValue(), 0,
			new HashMap<IoType, Integer>() {{
				put(IoType.CREATE, concurrency.getValue());
				put(IoType.UPDATE, concurrency.getValue());
				put(IoType.READ, concurrency.getValue());
			}}
		);
	}
}
