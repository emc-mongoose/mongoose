package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.scenario.json.JsonScenario;
import com.emc.mongoose.tests.system.base.OldScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.EnvUtil;
import com.emc.mongoose.tests.system.util.HttpStorageMockUtil;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;

import org.apache.commons.csv.CSVRecord;

import org.apache.logging.log4j.Level;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 Created by andrey on 27.08.17.
 */
public class ConcatRandomItemRangesTest
extends OldScenarioTestBase {

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
		return Paths.get(getBaseDir(), DIR_EXAMPLE_SCENARIO, "json", "systest", "ConcatRandomItemRanges.json");
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

		final String node = httpStorageMocks.keySet().iterator().next();
		final LongAdder ioTraceRecCount = new LongAdder();
		final long avgDstItemContentSize = SRC_ITEMS_TO_CONCAT_MAX * itemSize.getValue().get()
			/ (2 * SRC_ITEMS_RANDOM_RANGES_COUNT);
		final Consumer<CSVRecord> ioTraceReqTestFunc = ioTraceRec -> {
			testIoTraceRecord(ioTraceRec, IoType.CREATE.ordinal(), itemSize.getValue());
			final String nextItemPath = ioTraceRec.get("ItemPath");
			final int nextContentLength = HttpStorageMockUtil.getContentLength(node, nextItemPath);
			assertEquals(
				"I/O trace req #" + ioTraceRecCount.sum() + ": invalid object \"" + nextItemPath + "\"",
				avgDstItemContentSize, nextContentLength, 0.9 * avgDstItemContentSize
			);
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
