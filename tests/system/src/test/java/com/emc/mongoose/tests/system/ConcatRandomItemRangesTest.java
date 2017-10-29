package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.EnvUtil;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

import com.github.akurilov.commons.system.SizeInBytes;
import org.apache.commons.csv.CSVRecord;

import org.apache.logging.log4j.Level;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import static org.junit.Assert.assertTrue;

/**
 Created by andrey on 27.08.17.
 */
public class ConcatRandomItemRangesTest
extends ScenarioTestBase {

	private static final int SRC_ITEMS_TO_CONCAT_MIN = 20;
	private static final int SRC_ITEMS_TO_CONCAT_MAX = 50;
	private static final int SRC_ITEMS_RANDOM_RANGES_COUNT = 10;

	private String itemsFile;
	private boolean finishedInTime;

	public ConcatRandomItemRangesTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(getBaseDir(), DIR_SCENARIO, "systest", "ConcatRandomItemRanges.json");
	}

	@Override
	public final void setUp()
	throws Exception {
		super.setUp();
		itemsFile = stepId + ".csv";
		EnvUtil.set("SRC_ITEMS_TO_CONCAT_FILE", itemsFile);
		scenario = new JsonScenario(config, scenarioPath.toFile());
		final Thread runner = new Thread(
			() -> {
				try {
					scenario.run();
				} catch(final Throwable t) {
					LogUtil.exception(Level.ERROR, t, "Failed to run the scenario");
				}
			}
		);
		runner.start();
		TimeUnit.MINUTES.timedJoin(runner, 2);
		finishedInTime = !runner.isAlive();
		runner.interrupt();
		LogUtil.flushAll();
		TimeUnit.SECONDS.sleep(10);
	}

	@Override
	public final void tearDown()
	throws Exception {
		super.tearDown();
		try {
			Files.delete(Paths.get(itemsFile));
		} catch(final Exception ignored) {
		}
	}

	@Override
	public final void test()
	throws Exception {

		final LongAdder ioTraceRecCount = new LongAdder();
		final SizeInBytes avgDstItemContentSize = new SizeInBytes(
			SRC_ITEMS_TO_CONCAT_MIN * 100, SRC_ITEMS_TO_CONCAT_MAX * 200, 1
		);
		final Consumer<CSVRecord> ioTraceReqTestFunc = ioTraceRec -> {
			testIoTraceRecord(ioTraceRec, IoType.CREATE.ordinal(), avgDstItemContentSize);
			ioTraceRecCount.increment();
		};
		testIoTraceLogRecords(ioTraceReqTestFunc);
		assertTrue(
			"There should be more than 1 record in the I/O trace log file",
			ioTraceRecCount.sum() > 1
		);

		assertTrue("Scenario didn't finished in time", finishedInTime);
	}
}
