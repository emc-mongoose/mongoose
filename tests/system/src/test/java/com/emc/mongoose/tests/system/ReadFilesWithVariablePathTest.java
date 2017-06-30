package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.tests.system.base.EnvConfiguredTestBase;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.tests.system.util.EnvUtil;
import static com.emc.mongoose.common.Constants.KEY_STEP_ID;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 Created by andrey on 13.06.17.
 */
public class ReadFilesWithVariablePathTest
extends EnvConfiguredScenarioTestBase {

	static {
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("atmos", "s3", "swift"));
		EXCLUDE_PARAMS.put(
			KEY_ENV_ITEM_DATA_SIZE,
			Arrays.asList(new SizeInBytes("1MB"), new SizeInBytes("100MB"), new SizeInBytes("10GB"))
		);
		STEP_NAME = ReadFilesWithVariablePathTest.class.getSimpleName();
		SCENARIO_PATH = Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "ReadFilesWithVariablePath.json"
		);
	}

	private static String FILE_OUTPUT_PATH;
	private static String STD_OUTPUT;

	private static final int EXPECTED_COUNT = 10000;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ThreadContext.put(KEY_STEP_ID, STEP_NAME);
		CONFIG_ARGS.add("--item-naming-radix=16");
		CONFIG_ARGS.add("--item-naming-length=16");
		EnvConfiguredScenarioTestBase.setUpClass();
		if(SKIP_FLAG) {
			return;
		}
		if(STORAGE_DRIVER_TYPE.equals(STORAGE_TYPE_FS)) {
			FILE_OUTPUT_PATH = Paths
				.get(Paths.get(PathUtil.getBaseDir()).getParent().toString(), STEP_NAME)
				.toString();
			EnvUtil.set("FILE_OUTPUT_PATH", FILE_OUTPUT_PATH);
		}
		SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
		STD_OUT_STREAM.startRecording();
		SCENARIO.run();
		LogUtil.flushAll();
		STD_OUTPUT = STD_OUT_STREAM.stopRecordingAndGet();
		TimeUnit.SECONDS.sleep(5);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		if(! SKIP_FLAG) {
			try {
				DirWithManyFilesDeleter.deleteExternal(FILE_OUTPUT_PATH);
			} catch(final Exception e) {
				e.printStackTrace(System.err);
			}
		}
		EnvConfiguredTestBase.tearDownClass();
	}

	@Test
	public void testMetricsLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		testMetricsLogRecords(
			getMetricsLogRecords(),
			IoType.READ, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			EXPECTED_COUNT, 0, CONFIG.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
	}

	@Test
	public void testTotalMetricsLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		testTotalMetricsLogRecord(
			getMetricsTotalLogRecords().get(0),
			IoType.READ, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE, EXPECTED_COUNT, 0
		);
	}

	@Test
	public void testMetricsStdout()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.READ, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			CONFIG.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
	}

	@Test
	public void testIoTraceLogFile()
	throws Exception {

		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertEquals(EXPECTED_COUNT, ioTraceRecords.size());

		// Item path should look like:
		// ${FILE_OUTPUT_PATH}/1/b/0123456789abcdef
		// ${FILE_OUTPUT_PATH}/b/fedcba9876543210
		final Pattern subPathPtrn = Pattern.compile("(/[0-9a-f]){1,2}/[0-9a-f]{16}");

		String nextFilePath;
		Matcher m;
		final int baseOutputPathLen = FILE_OUTPUT_PATH.length();
		for(final CSVRecord ioTraceRecord : ioTraceRecords) {
			testIoTraceRecord(ioTraceRecord, IoType.READ.ordinal(), ITEM_DATA_SIZE);
			nextFilePath = ioTraceRecord.get("ItemPath");
			assertTrue(nextFilePath.startsWith(FILE_OUTPUT_PATH));
			nextFilePath = nextFilePath.substring(baseOutputPathLen);
			m = subPathPtrn.matcher(nextFilePath);
			assertTrue(m.matches());
		}
	}
}
