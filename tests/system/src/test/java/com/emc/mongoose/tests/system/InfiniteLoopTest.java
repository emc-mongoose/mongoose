package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.ThreadContext;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 08.06.17.
 */
public class InfiniteLoopTest
extends EnvConfiguredScenarioTestBase {

	private static final int SCENARIO_TIMEOUT = 50;
	private static final int EXPECTED_STEP_TIME = 5;
	private static final int EXPECTED_LOOP_COUNT = SCENARIO_TIMEOUT / EXPECTED_STEP_TIME;

	static {
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("atmos", "fs", "swift"));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_COUNT, Arrays.asList(1));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, Arrays.asList(1, 10));
		EXCLUDE_PARAMS.put(
			KEY_ENV_ITEM_DATA_SIZE,
			Arrays.asList(
				new SizeInBytes(0), new SizeInBytes("1MB"), new SizeInBytes("100MB"),
				new SizeInBytes("10GB")
			)
		);
		STEP_NAME = InfiniteLoopTest.class.getSimpleName();
		SCENARIO_PATH = Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "InfiniteLoop.json"
		);
	}

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ThreadContext.put(KEY_STEP_NAME, STEP_NAME);
		CONFIG_ARGS.add("--item-output-path=/default");
		CONFIG_ARGS.add("--test-step-limit-time=" + EXPECTED_STEP_TIME);
		EnvConfiguredScenarioTestBase.setUpClass();
		if(SKIP_FLAG) {
			return;
		}
		SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
		final Thread runner = new Thread(() -> SCENARIO.run());
		runner.start();
		TimeUnit.SECONDS.timedJoin(runner, SCENARIO_TIMEOUT);
		runner.interrupt();
		TimeUnit.SECONDS.sleep(10);
		LoadJobLogFileManager.flushAll();
	}

	@Test
	public final void testTotalMetricsLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> totalRecs = getMetricsTotalLogRecords();
		assertEquals(EXPECTED_LOOP_COUNT, totalRecs.size());
	}
}
