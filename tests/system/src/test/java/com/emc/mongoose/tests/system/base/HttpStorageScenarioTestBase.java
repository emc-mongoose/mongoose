package com.emc.mongoose.tests.system.base;

import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.run.scenario.Scenario;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static com.emc.mongoose.run.scenario.Scenario.FNAME_DEFAULT_SCENARIO;

/**
 Created by andrey on 17.02.17.
 */
public class HttpStorageScenarioTestBase
extends HttpStorageTestBase {

	protected static final Path DEFAULT_SCENARIO_PATH = Paths.get(
		getBaseDir(), DIR_SCENARIO, FNAME_DEFAULT_SCENARIO
	);
	protected static Path SCENARIO_PATH;
	protected static Scenario SCENARIO;
	protected static final int STORAGE_DRIVERS_COUNT = 1;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		HttpStorageTestBase.setUpClass();
		final String scenarioValue = CONFIG.getScenarioConfig().getFile();
		if(scenarioValue != null && !scenarioValue.isEmpty()) {
			SCENARIO_PATH = Paths.get(scenarioValue);
		} else {
			SCENARIO_PATH = DEFAULT_SCENARIO_PATH;
		}
		SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		SCENARIO.close();
		HttpStorageTestBase.tearDownClass();
	}

}
