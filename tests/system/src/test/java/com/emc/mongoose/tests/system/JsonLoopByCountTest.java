package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.scenario.json.JsonScenario;
import com.emc.mongoose.tests.system.base.OldScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.logging.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;

import org.apache.commons.csv.CSVRecord;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 07.06.17.
 */
public final class JsonLoopByCountTest
extends OldScenarioTestBase {

	private static final int EXPECTED_LOOP_COUNT = 10;
	private static final int EXPECTED_STEP_TIME = 5;

	private long actualTestTime;

	public JsonLoopByCountTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected String makeStepId() {
		return JsonLoopByCountTest.class.getSimpleName() + '-' + storageType.name() + '-' +
			driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(getBaseDir(), DIR_EXAMPLE_SCENARIO, "json", "systest", "LoopByCount.json");
	}

	@Before
	public final void setUp()
	throws Exception {
		configArgs.add("--test-step-limit-time=" + EXPECTED_STEP_TIME);
		super.setUp();
		scenario = new JsonScenario(config, scenarioPath.toFile());
		actualTestTime = System.currentTimeMillis();
		scenario.run();
		actualTestTime = (System.currentTimeMillis() - actualTestTime) / 1000;
		TimeUnit.SECONDS.sleep(10);
		LogUtil.flushAll();
	}

	@After
	public final void tearDown()
	throws Exception {
		super.tearDown();
	}

	@Override
	public void test()
	throws Exception {

		final List<CSVRecord> totalRecs = getMetricsTotalLogRecords();
		assertEquals(EXPECTED_LOOP_COUNT, totalRecs.size());
		for(final CSVRecord totalRec : totalRecs) {
			testTotalMetricsLogRecord(
				totalRec, IoType.CREATE, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(), 0,
				EXPECTED_STEP_TIME
			);
		}

		assertEquals(EXPECTED_LOOP_COUNT * EXPECTED_STEP_TIME, actualTestTime, 25);
	}
}
