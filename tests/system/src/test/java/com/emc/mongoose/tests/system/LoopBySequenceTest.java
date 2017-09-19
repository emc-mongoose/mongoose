package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.scenario.json.JsonScenario;
import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.scenario.Constants.DIR_SCENARIOS;

import org.apache.commons.csv.CSVRecord;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 08.06.17.
 */
public final class LoopBySequenceTest
extends ScenarioTestBase {

	private static final int EXPECTED_LOOP_COUNT = 4;
	private static final int EXPECTED_STEP_TIME = 15;

	private long actualTestTime;
	private String itemOutputPath;

	public LoopBySequenceTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected String makeStepId() {
		return LoopBySequenceTest.class.getSimpleName() + '-' + storageType.name() + '-' +
			driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(getBaseDir(), DIR_SCENARIOS, "json", "systest", "LoopBySequence.json");
	}

	@Before
	public final void setUp()
	throws Exception {
		configArgs.add("--test-step-limit-time=" + EXPECTED_STEP_TIME);
		super.setUp();
		itemOutputPath = "/default";
		switch(storageType) {
			case FS:
				itemOutputPath = Paths.get(
					Paths.get(PathUtil.getBaseDir()).getParent().toString(), stepId
				).toString();
				break;
			case SWIFT:
				config.getStorageConfig().getNetConfig().getHttpConfig().setNamespace("ns1");
				break;
		}
		config.getItemConfig().getOutputConfig().setPath(itemOutputPath);
		scenario = new JsonScenario(config, scenarioPath.toFile());
		actualTestTime = System.currentTimeMillis();
		scenario.run();
		actualTestTime = (System.currentTimeMillis() - actualTestTime) / 1000;
		TimeUnit.SECONDS.sleep(15);
		LogUtil.flushAll();
	}

	@After
	public final void tearDown()
	throws Exception {
		if(StorageType.FS.equals(storageType)) {
			try {
				DirWithManyFilesDeleter.deleteExternal(itemOutputPath);
			} catch(final Exception e) {
				e.printStackTrace(System.err);
			}
		}
		super.tearDown();
	}

	@Override
	public void test()
	throws Exception {

		assertEquals(EXPECTED_LOOP_COUNT * EXPECTED_STEP_TIME, actualTestTime, 10);

		final List<CSVRecord> totalRecs = getMetricsTotalLogRecords();
		assertEquals(EXPECTED_LOOP_COUNT, totalRecs.size());
		testTotalMetricsLogRecord(
			totalRecs.get(0), IoType.CREATE, 1, driverCount.getValue(), itemSize.getValue(), 0,
			EXPECTED_STEP_TIME
		);
		testTotalMetricsLogRecord(
			totalRecs.get(1), IoType.CREATE, 10, driverCount.getValue(), itemSize.getValue(), 0,
			EXPECTED_STEP_TIME
		);
		testTotalMetricsLogRecord(
			totalRecs.get(2), IoType.CREATE, 100, driverCount.getValue(), itemSize.getValue(), 0,
			EXPECTED_STEP_TIME
		);
		testTotalMetricsLogRecord(
			totalRecs.get(3), IoType.CREATE, 1000, driverCount.getValue(), itemSize.getValue(), 0,
			EXPECTED_STEP_TIME
		);
	}
}
