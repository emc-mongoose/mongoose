package com.emc.mongoose.tests.system.base;

import com.emc.mongoose.run.scenario.Scenario;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import org.junit.After;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static com.emc.mongoose.run.scenario.Scenario.FNAME_DEFAULT_SCENARIO;

/**
 Created by andrey on 11.08.17.
 */
public abstract class ScenarioTestBase
extends StorageTestBase {

	protected static final Path DEFAULT_SCENARIO_PATH = Paths.get(
		getBaseDir(), DIR_SCENARIO, FNAME_DEFAULT_SCENARIO
	);
	protected Path scenarioPath = null;
	protected Scenario scenario = null;

	protected ScenarioTestBase(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
		scenarioPath = makeScenarioPath();
		if(scenarioPath == null) {
			final String scenarioValue = config.getTestConfig().getScenarioConfig().getFile();
			if(scenarioValue != null && !scenarioValue.isEmpty()) {
				scenarioPath = Paths.get(scenarioValue);
			} else {
				scenarioPath = DEFAULT_SCENARIO_PATH;
			}
		}
	}

	protected abstract Path makeScenarioPath();

	@After
	public void tearDown()
	throws Exception {
		if(scenario != null) {
			scenario.close();
			scenario = null;
		}
		super.tearDown();
	}
}
