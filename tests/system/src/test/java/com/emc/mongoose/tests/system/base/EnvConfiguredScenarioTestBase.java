package com.emc.mongoose.tests.system.base;

import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static com.emc.mongoose.run.scenario.Scenario.FNAME_DEFAULT_SCENARIO;

import com.emc.mongoose.run.scenario.Scenario;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 Created by andrey on 04.06.17.
 */
public class EnvConfiguredScenarioTestBase
extends EnvConfiguredTestBase {

	protected static final Path DEFAULT_SCENARIO_PATH = Paths.get(
		getBaseDir(), DIR_SCENARIO, FNAME_DEFAULT_SCENARIO
	);
	protected static Path SCENARIO_PATH;
	protected static Scenario SCENARIO;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		EnvConfiguredTestBase.setUpClass();
		if(EXCLUDE_FLAG) {
			return;
		}
		final String scenarioValue = CONFIG.getTestConfig().getScenarioConfig().getFile();
		if(scenarioValue != null && !scenarioValue.isEmpty()) {
			SCENARIO_PATH = Paths.get(scenarioValue);
		} else {
			SCENARIO_PATH = DEFAULT_SCENARIO_PATH;
		}
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		if(SCENARIO != null) {
			SCENARIO.close();
			SCENARIO = null;
		}
		EnvConfiguredTestBase.tearDownClass();
	}
}
