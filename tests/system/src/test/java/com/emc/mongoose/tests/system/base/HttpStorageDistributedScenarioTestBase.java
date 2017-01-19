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
 Created by andrey on 19.01.17.
 */
public abstract class HttpStorageDistributedScenarioTestBase
extends HttpStorageDistributedTestBase {

	protected static Scenario SCENARIO;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		HttpStorageDistributedTestBase.setUpClass();
		final String scenarioValue = CONFIG.getScenarioConfig().getFile();
		final Path scenarioPath;
		if(scenarioValue != null && !scenarioValue.isEmpty()) {
			scenarioPath = Paths.get(scenarioValue);
		} else {
			scenarioPath = Paths.get(getBaseDir(), DIR_SCENARIO, FNAME_DEFAULT_SCENARIO);
		}
		SCENARIO = new JsonScenario(CONFIG, scenarioPath.toFile());
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		SCENARIO.close();
		HttpStorageDistributedTestBase.tearDownClass();
	}
}
