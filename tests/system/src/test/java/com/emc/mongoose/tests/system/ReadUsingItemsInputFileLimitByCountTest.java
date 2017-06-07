package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.nio.file.Paths;
import java.util.Arrays;

/**
 Created by andrey on 07.06.17.
 */
public class ReadUsingItemsInputFileLimitByCountTest
extends EnvConfiguredScenarioTestBase {

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
		try {
			SCENARIO.run();
		} catch(final Throwable t) {
			LogUtil.exception(Level.ERROR, t, "Failed to run the scenario");
		}
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		if(!EXCLUDE_FLAG) {
			// TODO
		}
		EnvConfiguredScenarioTestBase.tearDownClass();
	}
}
