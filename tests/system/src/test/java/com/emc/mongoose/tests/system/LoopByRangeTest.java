package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;

import java.nio.file.Paths;
import java.util.Arrays;

import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

/**
 Created by andrey on 08.06.17.
 */
public class LoopByRangeTest
extends EnvConfiguredScenarioTestBase {

	private static final int EXPECTED_LOOP_COUNT = 4;

	static {
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("atmos", "s3", "swift"));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, Arrays.asList(10, 100));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, Arrays.asList(true));
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

}
