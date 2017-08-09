package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.common.SizeInBytes;
import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 08.06.17.
 */
public class LoopBySequenceTest
extends EnvConfiguredScenarioTestBase {

	private static final int EXPECTED_LOOP_COUNT = 4;
	private static final int EXPECTED_STEP_TIME = 15;
	private static long ACTUAL_TEST_TIME;
	private static String ITEM_OUTPUT_PATH;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		EXCLUDE_PARAMS.clear();
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("atmos", "s3"));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_COUNT, Arrays.asList(2));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, Arrays.asList(10, 100, 1000));
		EXCLUDE_PARAMS.put(
			KEY_ENV_ITEM_DATA_SIZE,
			Arrays.asList(new SizeInBytes("1MB"), new SizeInBytes("100MB"), new SizeInBytes("10GB"))
		);
		STEP_ID = LoopBySequenceTest.class.getSimpleName();
		SCENARIO_PATH = Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "LoopBySequence.json"
		);
		ThreadContext.put(KEY_TEST_STEP_ID, STEP_ID);
		CONFIG_ARGS.add("--test-step-limit-time=" + EXPECTED_STEP_TIME);
		EnvConfiguredScenarioTestBase.setUpClass();
		if(SKIP_FLAG) {
			return;
		}
		ITEM_OUTPUT_PATH = "/default";
		switch(STORAGE_DRIVER_TYPE) {
			case STORAGE_TYPE_FS:
				ITEM_OUTPUT_PATH = Paths.get(
					Paths.get(PathUtil.getBaseDir()).getParent().toString(), STEP_ID
				).toString();
				break;
			case STORAGE_TYPE_SWIFT:
				CONFIG.getStorageConfig().getNetConfig().getHttpConfig().setNamespace("ns1");
				break;
		}
		CONFIG.getItemConfig().getOutputConfig().setPath(ITEM_OUTPUT_PATH);
		SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
		ACTUAL_TEST_TIME = System.currentTimeMillis();
		SCENARIO.run();
		ACTUAL_TEST_TIME = (System.currentTimeMillis() - ACTUAL_TEST_TIME) / 1000;
		TimeUnit.SECONDS.sleep(15);
		LogUtil.flushAll();
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		if(! SKIP_FLAG) {
			if(STORAGE_TYPE_FS.equals(STORAGE_DRIVER_TYPE)) {
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
	public final void testDuration()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		assertEquals(EXPECTED_LOOP_COUNT * EXPECTED_STEP_TIME, ACTUAL_TEST_TIME, 10);
	}

	@Test
	public final void testTotalMetricsLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> totalRecs = getMetricsTotalLogRecords();
		assertEquals(EXPECTED_LOOP_COUNT, totalRecs.size());
		testTotalMetricsLogRecord(
			totalRecs.get(0), IoType.CREATE, 1, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE, 0,
			EXPECTED_STEP_TIME
		);
		testTotalMetricsLogRecord(
			totalRecs.get(1), IoType.CREATE, 10, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE, 0,
			EXPECTED_STEP_TIME
		);
		testTotalMetricsLogRecord(
			totalRecs.get(2), IoType.CREATE, 100, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE, 0,
			EXPECTED_STEP_TIME
		);
		testTotalMetricsLogRecord(
			totalRecs.get(3), IoType.CREATE, 1000, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE, 0,
			EXPECTED_STEP_TIME
		);
	}
}
