package com.emc.mongoose.tests.system.base;

import com.emc.mongoose.scenario.json.Scenario;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;

import org.junit.After;
import org.junit.Before;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 Created by andrey on 11.08.17.
 */
public abstract class OldScenarioTestBase
extends StorageTestBase {

	protected static final Path DEFAULT_SCENARIO_PATH = Paths.get(
		getBaseDir(), DIR_EXAMPLE_SCENARIO, "json", "default.json"
	);
	protected Path scenarioPath = null;
	protected Scenario scenario = null;

	protected OldScenarioTestBase(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Before
	public void setUp()
	throws Exception {
		super.setUp();
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

	@After
	public void tearDown()
	throws Exception {
		if(scenario != null) {
			scenario.close();
			scenario = null;
		}
		super.tearDown();
	}

	protected abstract Path makeScenarioPath();
}
