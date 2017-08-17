package com.emc.mongoose.tests.system;

import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

import org.apache.commons.csv.CSVRecord;

import org.junit.Before;
import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 08.06.17.
 */
public class InfiniteLoopTest
extends ScenarioTestBase {

	private static final int SCENARIO_TIMEOUT = 50;
	private static final int EXPECTED_STEP_TIME = 5;
	private static final int EXPECTED_LOOP_COUNT = SCENARIO_TIMEOUT / EXPECTED_STEP_TIME - 1;

	public InfiniteLoopTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(getBaseDir(), DIR_SCENARIO, "systest", "InfiniteLoop.json");
	}

	@Override
	protected String makeStepId() {
		return InfiniteLoopTest.class.getSimpleName() + '-' + storageType.name() + '-' +
			driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}

	@Before
	public void setUp()
	throws Exception {
		configArgs.add("--item-output-path=/default");
		configArgs.add("--test-step-limit-time=" + EXPECTED_STEP_TIME);
		super.setUp();
		scenario = new JsonScenario(config, scenarioPath.toFile());
		final Thread runner = new Thread(() -> scenario.run());
		runner.start();
		TimeUnit.SECONDS.timedJoin(runner, SCENARIO_TIMEOUT);
		runner.interrupt();
		runner.join();
		TimeUnit.SECONDS.sleep(10);
		LogUtil.flushAll();
	}

	@Override
	public void test()
	throws Exception {
		final List<CSVRecord> totalRecs = getMetricsTotalLogRecords();
		assertEquals(
			"Expected steps count: " + EXPECTED_LOOP_COUNT + ", but was: " + totalRecs.size(),
			EXPECTED_LOOP_COUNT, totalRecs.size(), 1
		);
	}
}
