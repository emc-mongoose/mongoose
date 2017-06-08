package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.tests.system.util.LogPatterns;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.ThreadContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 Created by andrey on 08.06.17.
 */
public class LoopByRangeTest
extends EnvConfiguredScenarioTestBase {

	private static final double EXPECTED_LOOP_START_VALUE = 2.71828182846;
	private static final double EXPECTED_LOOP_LIMIT_VALUE = 3.1415926;
	private static final double EXPECTED_STEP_VALUE = 0.1;
	private static final Pattern PTRN_LOOP_STEP_MSG = Pattern.compile(
		LogPatterns.ASCII_COLOR.pattern() + LogPatterns.DATE_TIME_ISO8601.pattern() +
			"\\s+" + LogPatterns.STD_OUT_LOG_LEVEL.pattern() +
			"\\s+" + LogPatterns.STD_OUT_CLASS_NAME.pattern() +
			"\\s+" + LogPatterns.STD_OUT_THREAD_NAME.pattern() +
			"\\s+Use\\snext\\svalue\\sfor\\s\"i\":\\s(?<stepValue>\\d\\.\\d+)"
	);

	private static String STD_OUTPUT;

	static {
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("atmos", "s3", "swift"));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, Arrays.asList(10, 100));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_REMOTE, Arrays.asList(true));
		EXCLUDE_PARAMS.put(
			KEY_ENV_ITEM_DATA_SIZE,
			Arrays.asList(
				new SizeInBytes("1KB"), new SizeInBytes("1MB"), new SizeInBytes("100MB"),
				new SizeInBytes("10GB")
			)
		);
		STEP_NAME = LoopByCountTest.class.getSimpleName();
		SCENARIO_PATH = Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "LoopByRange.json"
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
		SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
		STD_OUT_STREAM.startRecording();
		SCENARIO.run();
		LoadJobLogFileManager.flushAll();
		STD_OUTPUT = STD_OUT_STREAM.stopRecordingAndGet();
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		EnvConfiguredScenarioTestBase.tearDownClass();
	}

	@Test
	public final void testSteps()
	throws Exception {
		if(EXCLUDE_FLAG) {
			return;
		}
		final Matcher m = PTRN_LOOP_STEP_MSG.matcher(STD_OUTPUT);
		double nextExpectedStepVal = EXPECTED_LOOP_START_VALUE;
		while(m.find()) {
			final String t = m.group("stepValue");
			assertEquals(nextExpectedStepVal, Double.parseDouble(t), EXPECTED_STEP_VALUE / 100);
			nextExpectedStepVal += EXPECTED_STEP_VALUE;
		}
	}
}
